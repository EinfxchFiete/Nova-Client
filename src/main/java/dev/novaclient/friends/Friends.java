package dev.novaclient.friends;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.novaclient.NovaClient;
import dev.novaclient.util.Notifications;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jspecify.annotations.Nullable;

/**
 * The friend list (saved in config/novaclient/friends.json) and what the Nova server says about the friends.
 * Adding someone = sending a request; once they add you too, you are friends and see each other's online status
 * and server. The server only shows that to mutual friends.
 */
public final class Friends {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("novaclient").resolve("friends.json");
	public static final int MAX = 200;

	public record Friend(UUID uuid, String name) {
	}

	/** What the Nova server knows: mutual friendship, online, server they play on, ms since last seen. */
	public record Status(boolean mutual, String name, boolean online, String server, long lastSeenMs, long receivedAt) {
	}

	public record Request(UUID uuid, String name) {
	}

	private static final Map<UUID, Friend> LIST = new LinkedHashMap<>();
	private static final Set<UUID> IGNORED = new HashSet<>();
	private static volatile Map<UUID, Status> status = Map.of();
	private static volatile List<Request> requests = List.of();
	private static boolean loaded;

	private Friends() {
	}

	private static synchronized void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		try {
			if (!Files.exists(FILE)) {
				return;
			}
			JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement e : root.getAsJsonArray("friends")) {
				JsonObject o = e.getAsJsonObject();
				UUID id = UUID.fromString(o.get("uuid").getAsString());
				LIST.put(id, new Friend(id, o.get("name").getAsString()));
			}
			if (root.has("ignored")) {
				for (JsonElement e : root.getAsJsonArray("ignored")) {
					IGNORED.add(UUID.fromString(e.getAsString()));
				}
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not read friends.json", e);
		}
	}

	private static synchronized void save() {
		JsonObject root = new JsonObject();
		JsonArray arr = new JsonArray();
		for (Friend f : LIST.values()) {
			JsonObject o = new JsonObject();
			o.addProperty("uuid", f.uuid().toString());
			o.addProperty("name", f.name());
			arr.add(o);
		}
		root.add("friends", arr);
		JsonArray ign = new JsonArray();
		IGNORED.forEach(u -> ign.add(u.toString()));
		root.add("ignored", ign);
		try {
			Files.createDirectories(FILE.getParent());
			Path tmp = FILE.resolveSibling("friends.json.tmp");
			Files.writeString(tmp, root.toString(), StandardCharsets.UTF_8);
			Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			NovaClient.LOGGER.warn("Could not save friends.json", e);
		}
	}

	public static synchronized List<Friend> list() {
		load();
		return List.copyOf(LIST.values());
	}

	public static synchronized boolean isFriend(UUID uuid) {
		load();
		return uuid != null && LIST.containsKey(uuid);
	}

	/** Friend who also added you (known from the Nova server). */
	public static boolean isMutual(UUID uuid) {
		Status s = status.get(uuid);
		return isFriend(uuid) && s != null && s.mutual();
	}

	public static synchronized boolean add(UUID uuid, String name) {
		load();
		Minecraft mc = Minecraft.getInstance();
		if (uuid == null || LIST.size() >= MAX || mc.player != null && uuid.equals(mc.player.getUUID())) {
			return false;
		}
		LIST.put(uuid, new Friend(uuid, name));
		IGNORED.remove(uuid);
		save();
		return true;
	}

	public static synchronized void remove(UUID uuid) {
		load();
		LIST.remove(uuid);
		save();
	}

	/** Hides a request until the player adds you again later. */
	public static synchronized void ignore(UUID uuid) {
		load();
		IGNORED.add(uuid);
		save();
	}

	public static @Nullable Status status(UUID uuid) {
		return status.get(uuid);
	}

	public static List<Request> requests() {
		List<Request> out = new ArrayList<>();
		for (Request r : requests) {
			if (!IGNORED.contains(r.uuid()) && !isFriend(r.uuid())) {
				out.add(r);
			}
		}
		return out;
	}

	public static int onlineCount() {
		int n = 0;
		for (Friend f : list()) {
			Status s = status.get(f.uuid());
			if (s != null && s.mutual() && s.online()) {
				n++;
			}
		}
		return n;
	}

	/** Called by the network thread with the server's answer. */
	public static void update(@Nullable JsonObject friends, @Nullable JsonArray reqs, boolean notify) {
		long now = System.currentTimeMillis();
		Map<UUID, Status> next = new LinkedHashMap<>();
		if (friends != null) {
			for (var e : friends.entrySet()) {
				try {
					UUID id = UUID.fromString(e.getKey());
					JsonObject o = e.getValue().getAsJsonObject();
					boolean mutual = o.has("mutual") && o.get("mutual").getAsBoolean();
					String name = o.has("name") ? clean(o.get("name").getAsString(), 16) : "";
					boolean online = mutual && o.has("online") && o.get("online").getAsBoolean();
					String server = o.has("server") ? clean(o.get("server").getAsString(), 70) : "";
					long seen = o.has("lastSeen") ? o.get("lastSeen").getAsLong() : -1;
					next.put(id, new Status(mutual, name, online, server, seen, now));
				} catch (RuntimeException ignored) {
					// skip malformed entries
				}
			}
		}
		List<Request> nextReq = new ArrayList<>();
		if (reqs != null) {
			for (JsonElement e : reqs) {
				try {
					JsonObject o = e.getAsJsonObject();
					nextReq.add(new Request(UUID.fromString(o.get("uuid").getAsString()), clean(o.get("name").getAsString(), 16)));
				} catch (RuntimeException ignored) {
					// skip malformed entries
				}
			}
		}
		Map<UUID, Status> before = status;
		List<Request> beforeReq = requests;
		status = next;
		requests = List.copyOf(nextReq);
		if (!notify) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		for (var e : next.entrySet()) {
			Status old = before.get(e.getKey());
			if (!before.isEmpty() && e.getValue().online() && (old == null || !old.online())) {
				mc.execute(() -> Notifications.push(e.getValue().name() + " ist jetzt online", 0));
			}
			// keep the stored name up to date (players can change their name)
			Friend f = list().stream().filter(x -> x.uuid().equals(e.getKey())).findFirst().orElse(null);
			if (f != null && !e.getValue().name().isEmpty() && !e.getValue().name().equals(f.name())) {
				mc.execute(() -> add(f.uuid(), e.getValue().name()));
			}
		}
		for (Request r : nextReq) {
			if (beforeReq.stream().noneMatch(x -> x.uuid().equals(r.uuid())) && !IGNORED.contains(r.uuid()) && !isFriend(r.uuid())) {
				mc.execute(() -> Notifications.push(r.name() + " möchte dein Freund sein", 0));
			}
		}
	}

	static String clean(String s, int max) {
		String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}\\u00a7]", "").trim();
		return t.length() > max ? t.substring(0, max) : t;
	}

	/**
	 * Finds a player by name: first in the tab list of the current server, otherwise via Mojang (in the background).
	 * The callback runs on the game thread with the profile or null.
	 */
	public static void lookup(String name, Consumer<@Nullable Friend> callback) {
		Minecraft mc = Minecraft.getInstance();
		String n = name.trim();
		if (!n.matches("[A-Za-z0-9_]{1,16}")) {
			callback.accept(null);
			return;
		}
		ClientPacketListener connection = mc.getConnection();
		if (connection != null) {
			for (PlayerInfo info : connection.getOnlinePlayers()) {
				if (info.getProfile().name().equalsIgnoreCase(n)) {
					callback.accept(new Friend(info.getProfile().id(), info.getProfile().name()));
					return;
				}
			}
		}
		CompletableFuture.supplyAsync(() -> {
			try {
				Optional<GameProfile> p = mc.services().profileResolver().fetchByName(n);
				return p.map(g -> new Friend(g.id(), g.name())).orElse(null);
			} catch (RuntimeException e) {
				return null;
			}
		}).completeOnTimeout(null, 10, java.util.concurrent.TimeUnit.SECONDS) // Mojang not reachable: don't wait forever
			.thenAccept(f -> mc.execute(() -> callback.accept(f)));
	}

	/** Lower-case helper for searching. */
	public static String key(String s) {
		return s.toLowerCase(Locale.ROOT);
	}
}
