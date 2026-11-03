package dev.novaclient.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novaclient.NovaClient;
import dev.novaclient.cosmetics.Cosmetics;
import dev.novaclient.cosmetics.Loadout;
import dev.novaclient.emote.Emotes;
import dev.novaclient.module.utility.NetworkModule;
import dev.novaclient.music.NowPlaying;
import dev.novaclient.module.hud.MusicModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Talks to the Nova server so Nova players see each other's cosmetics, songs and emotes.
 * Login works like joining a Minecraft server: the client calls Mojang's joinServer with a nonce from the
 * Nova server, the Nova server checks it with Mojang's hasJoined. The Microsoft/Minecraft token itself is
 * only ever sent to Mojang, never to the Nova server.
 */
public final class NovaNetwork {
	private static final long SYNC_INTERVAL_MS = 3000L;
	private static final long STALE_MS = 20_000L;

	/** What another Nova player wears / plays right now. */
	public record RemoteStatus(Loadout loadout, String songTitle, String songArtist, long songPos, long songLen, boolean songPlaying,
		String emote, long emoteStart, long receivedAt) {
	}

	/** Data prepared on the game thread for the next sync. */
	private record Snapshot(String server, UUID uuid, String name, String accessToken, JsonObject status, List<UUID> players,
		List<UUID> friends, boolean notifyFriends) {
	}

	private static final Map<UUID, RemoteStatus> STATUS = new ConcurrentHashMap<>();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
	private static ScheduledExecutorService executor;

	private static volatile Snapshot pending;
	private static volatile String token;
	private static volatile UUID tokenUuid;
	private static volatile String tokenServer;
	private static volatile long nextAuthAttempt;
	private static volatile long authBackoff = 30_000L;
	private static volatile long lastSync;
	private static volatile boolean syncRequested;
	private static volatile boolean connected;
	private static volatile String lastError = "";

	private NovaNetwork() {
	}

	public static synchronized void start() {
		if (executor != null) {
			return;
		}
		executor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "Nova Network");
			t.setDaemon(true);
			return t;
		});
		executor.scheduleWithFixedDelay(NovaNetwork::loop, 1, 1, TimeUnit.SECONDS);
	}

	/** Called every client tick on the game thread: prepares the data for the network thread. */
	public static void clientTick(Minecraft mc) {
		NetworkModule net = NetworkModule.get();
		String server = net == null ? "" : net.serverUrl();
		if (net == null || !net.isEnabled() || server.isEmpty() || mc.player == null || mc.getConnection() == null) {
			pending = null;
			if (!STATUS.isEmpty()) {
				STATUS.clear();
			}
			connected = false;
			return;
		}
		JsonObject status = new JsonObject();
		status.add("cosmetics", Cosmetics.local().toJson());
		NowPlaying.Song song = MusicModule.sharedSong();
		if (song != null) {
			JsonObject s = new JsonObject();
			s.addProperty("title", song.title());
			s.addProperty("artist", song.artist());
			if (song.hasTimeline()) {
				s.addProperty("pos", song.positionNow());
				s.addProperty("len", song.lengthMs());
				s.addProperty("playing", song.playing());
			}
			status.add("song", s);
		}
		Emotes.Active emote = Emotes.localActive();
		if (emote != null) {
			JsonObject e = new JsonObject();
			e.addProperty("id", emote.id());
			e.addProperty("age", Math.max(0, System.currentTimeMillis() - emote.start()));
			status.add("emote", e);
		}
		List<UUID> players = new ArrayList<>();
		ClientPacketListener connection = mc.getConnection();
		for (PlayerInfo info : connection.getListedOnlinePlayers()) {
			UUID id = info.getProfile().id();
			if (!id.equals(mc.player.getUUID()) && players.size() < 200) {
				players.add(id);
			}
		}
		// players that are rendered but not listed (e.g. hidden tab list) are asked for too
		for (var p : mc.level.players()) {
			if (p != mc.player && !players.contains(p.getUUID()) && players.size() < 200) {
				players.add(p.getUUID());
			}
		}
		// friends: the list goes to the server so it can tell mutual friends that you are online (and where, if shared)
		dev.novaclient.module.utility.FriendsModule fm = dev.novaclient.module.utility.FriendsModule.get();
		boolean friendsOn = fm != null && fm.isEnabled();
		List<UUID> friends = new ArrayList<>();
		if (friendsOn) {
			dev.novaclient.friends.Friends.list().forEach(f -> friends.add(f.uuid()));
			if (fm.shareServer.isOn()) {
				var sd = mc.getCurrentServer();
				status.addProperty("server", mc.getSingleplayerServer() != null ? "Einzelspieler" : sd != null ? sd.ip : "");
			}
		}
		pending = new Snapshot(server, mc.getUser().getProfileId(), mc.getUser().getName(), mc.getUser().getAccessToken(), status, players,
			friendsOn ? friends : null, friendsOn && fm.notify.isOn());
	}

	/** Ask for a sync as soon as possible (e.g. when an emote starts or the outfit changed). */
	public static void requestSync() {
		syncRequested = true;
	}

	private static void loop() {
		Snapshot s = pending;
		if (s == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (!syncRequested && now - lastSync < SYNC_INTERVAL_MS) {
			return;
		}
		syncRequested = false;
		lastSync = now;
		try {
			if (token == null || !s.uuid().equals(tokenUuid) || !s.server().equals(tokenServer)) {
				token = null;
				if (now < nextAuthAttempt) {
					return;
				}
				authenticate(s);
			}
			sync(s);
		} catch (Exception e) {
			connected = false;
			lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		}
	}

	private static JsonObject post(String url, JsonObject body, String bearer) throws Exception {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(10))
			.header("Content-Type", "application/json")
			.header("User-Agent", NovaClient.NAME + "/" + NovaClient.VERSION)
			.POST(HttpRequest.BodyPublishers.ofString(body.toString()));
		if (bearer != null) {
			b.header("Authorization", "Bearer " + bearer);
		}
		HttpResponse<String> res = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
		if (res.statusCode() == 401) {
			token = null;
			throw new IllegalStateException("Sitzung abgelaufen");
		}
		if (res.statusCode() != 200) {
			throw new IllegalStateException("HTTP " + res.statusCode());
		}
		JsonElement el = JsonParser.parseString(res.body());
		return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
	}

	private static void authenticate(Snapshot s) throws Exception {
		JsonObject start = new JsonObject();
		start.addProperty("name", s.name());
		JsonObject r = post(s.server() + "/v1/auth/start", start, null);
		String serverId = r.get("serverId").getAsString();
		try {
			// proves to Mojang (not to us) that we own this account
			Minecraft.getInstance().services().sessionService().joinServer(s.uuid(), s.accessToken(), serverId);
		} catch (Exception e) {
			nextAuthAttempt = System.currentTimeMillis() + authBackoff;
			authBackoff = Math.min(authBackoff * 2, 15 * 60_000L);
			throw new IllegalStateException("Mojang-Anmeldung fehlgeschlagen");
		}
		JsonObject finish = new JsonObject();
		finish.addProperty("name", s.name());
		finish.addProperty("serverId", serverId);
		JsonObject f = post(s.server() + "/v1/auth/finish", finish, null);
		token = f.get("token").getAsString();
		tokenUuid = s.uuid();
		tokenServer = s.server();
		authBackoff = 30_000L;
	}

	private static void sync(Snapshot s) throws Exception {
		JsonObject body = new JsonObject();
		body.add("status", s.status());
		JsonArray players = new JsonArray();
		for (UUID id : s.players()) {
			players.add(id.toString());
		}
		body.add("players", players);
		if (s.friends() != null) {
			JsonArray friends = new JsonArray();
			for (UUID id : s.friends()) {
				friends.add(id.toString());
			}
			body.add("friends", friends);
		}
		JsonObject res = post(s.server() + "/v1/sync", body, token);
		if (s.friends() != null) {
			dev.novaclient.friends.Friends.update(res.has("friends") && res.get("friends").isJsonObject() ? res.getAsJsonObject("friends") : null,
				res.has("requests") && res.get("requests").isJsonArray() ? res.getAsJsonArray("requests") : null, s.notifyFriends());
		}
		long now = System.currentTimeMillis();
		Map<UUID, RemoteStatus> fresh = new ConcurrentHashMap<>();
		if (res.has("players") && res.get("players").isJsonObject()) {
			for (Map.Entry<String, JsonElement> e : res.getAsJsonObject("players").entrySet()) {
				try {
					UUID id = UUID.fromString(e.getKey());
					fresh.put(id, parseStatus(e.getValue().getAsJsonObject(), now));
				} catch (RuntimeException ignored) {
					// skip malformed entries
				}
			}
		}
		STATUS.keySet().retainAll(fresh.keySet());
		STATUS.putAll(fresh);
		connected = true;
		lastError = "";
	}

	private static RemoteStatus parseStatus(JsonObject o, long now) {
		Loadout loadout = o.has("cosmetics") && o.get("cosmetics").isJsonObject() ? Loadout.fromJson(o.getAsJsonObject("cosmetics")) : Loadout.NONE;
		String title = "";
		String artist = "";
		long pos = -1;
		long len = -1;
		boolean playing = true;
		if (o.has("song") && o.get("song").isJsonObject()) {
			JsonObject s = o.getAsJsonObject("song");
			title = clean(s.has("title") ? s.get("title").getAsString() : "", 80);
			artist = clean(s.has("artist") ? s.get("artist").getAsString() : "", 60);
			if (s.has("len") && s.has("pos")) {
				len = s.get("len").getAsLong();
				pos = s.get("pos").getAsLong();
				if (len <= 0 || len > 24L * 3600_000L || pos < 0) {
					len = -1;
					pos = -1;
				}
				playing = !s.has("playing") || s.get("playing").getAsBoolean();
			}
		}
		String emote = "";
		long emoteStart = 0;
		if (o.has("emote") && o.get("emote").isJsonObject()) {
			JsonObject e = o.getAsJsonObject("emote");
			emote = Emotes.valid(e.has("id") ? e.get("id").getAsString() : "");
			emoteStart = now - Math.max(0, Math.min(60_000L, e.has("age") ? e.get("age").getAsLong() : 0));
		}
		return new RemoteStatus(loadout, title, artist, pos, Math.max(len, -1), playing, emote, emoteStart, now);
	}

	/** Removes control characters and limits the length of text from other players. */
	static String clean(String s, int max) {
		String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}\\u00a7]", "").trim();
		return t.length() > max ? t.substring(0, max) : t;
	}

	private static RemoteStatus fresh(UUID uuid) {
		RemoteStatus s = STATUS.get(uuid);
		if (s == null || System.currentTimeMillis() - s.receivedAt() > STALE_MS) {
			return null;
		}
		return s;
	}

	// ------------------------------------------------------------------ queries used by rendering

	public static boolean isNovaUser(UUID uuid) {
		return fresh(uuid) != null;
	}

	public static Loadout loadout(UUID uuid) {
		NetworkModule net = NetworkModule.get();
		if (net == null || !net.showOthers.isOn()) {
			return null;
		}
		RemoteStatus s = fresh(uuid);
		return s == null ? null : s.loadout();
	}

	/** "Title – Artist" of another Nova player, or null. */
	public static String song(UUID uuid) {
		RemoteStatus s = fresh(uuid);
		if (s == null || s.songTitle().isEmpty()) {
			return null;
		}
		return s.songArtist().isEmpty() ? s.songTitle() : s.songTitle() + " – " + s.songArtist();
	}

	/** The song another Nova player listens to (with extrapolated position), or null. */
	public static NowPlaying.Song songInfo(UUID uuid) {
		RemoteStatus s = fresh(uuid);
		if (s == null || s.songTitle().isEmpty()) {
			return null;
		}
		return new NowPlaying.Song(s.songTitle(), s.songArtist(), s.songPlaying(), "", s.songPos(), s.songLen(), s.receivedAt());
	}

	public static RemoteStatus status(UUID uuid) {
		return fresh(uuid);
	}

	public static boolean isConnected() {
		return connected;
	}

	public static String lastError() {
		return lastError;
	}

	public static int knownPlayers() {
		return STATUS.size();
	}

	// ------------------------------------------------------------------ test hooks

	/** Used by the automated tests (no Microsoft login available there). */
	public static void setSessionForTest(String server, UUID uuid, String testToken) {
		token = testToken;
		tokenUuid = uuid;
		tokenServer = server;
		syncRequested = true;
	}
}
