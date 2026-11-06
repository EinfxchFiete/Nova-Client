package dev.novaclient.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novaclient.NovaClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

/**
 * Remembered music data in config/novaclient/music.json: lyrics offset and chosen lyrics per song, songs without lyrics
 * display, the recently played songs and how often / how long each song was played.
 */
public final class MusicData {
	private static final int MAX_RECENT = 50;
	private static final int MAX_STATS = 500;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public record Played(String title, String artist, String source, long at) {
	}

	public record Stat(String title, String artist, int plays, long listenedMs, long lastAt) {
	}

	private static final Map<String, Long> OFFSETS = new HashMap<>();
	private static final Map<String, Long> CHOICES = new HashMap<>();
	private static final Set<String> HIDDEN = new HashSet<>();
	private static final List<Played> RECENT = new ArrayList<>();
	private static final Map<String, Stat> STATS = new HashMap<>();
	private static boolean loaded;
	private static boolean dirty;
	private static long dirtySince;
	private static @Nullable Path fileOverride;

	private MusicData() {
	}

	/** Song key: lower case "title\0artist". */
	public static String key(String title, String artist) {
		return (title + "\u0000" + artist).toLowerCase(java.util.Locale.ROOT);
	}

	private static Path file() {
		return fileOverride != null ? fileOverride : FabricLoader.getInstance().getConfigDir().resolve("novaclient").resolve("music.json");
	}

	/** For tests: use another file and forget what was loaded. */
	public static synchronized void useFileForTest(Path path) {
		fileOverride = path;
		loaded = false;
		OFFSETS.clear();
		CHOICES.clear();
		HIDDEN.clear();
		RECENT.clear();
		STATS.clear();
	}

	private static synchronized void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path f = file();
		if (!Files.isRegularFile(f)) {
			return;
		}
		try {
			JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
			if (o.has("offsets")) {
				for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("offsets").entrySet()) {
					OFFSETS.put(e.getKey(), e.getValue().getAsLong());
				}
			}
			if (o.has("choices")) {
				for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("choices").entrySet()) {
					CHOICES.put(e.getKey(), e.getValue().getAsLong());
				}
			}
			if (o.has("hidden")) {
				for (JsonElement e : o.getAsJsonArray("hidden")) {
					HIDDEN.add(e.getAsString());
				}
			}
			if (o.has("recent")) {
				for (JsonElement e : o.getAsJsonArray("recent")) {
					JsonObject r = e.getAsJsonObject();
					RECENT.add(new Played(str(r, "title"), str(r, "artist"), str(r, "source"), r.has("at") ? r.get("at").getAsLong() : 0));
				}
			}
			if (o.has("stats")) {
				for (JsonElement e : o.getAsJsonArray("stats")) {
					JsonObject r = e.getAsJsonObject();
					Stat s = new Stat(str(r, "title"), str(r, "artist"), r.get("plays").getAsInt(), r.get("ms").getAsLong(),
						r.has("last") ? r.get("last").getAsLong() : 0);
					STATS.put(key(s.title(), s.artist()), s);
				}
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not read {}", f, e);
		}
	}

	private static String str(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}

	private static void changed() {
		if (!dirty) {
			dirtySince = System.currentTimeMillis();
		}
		dirty = true;
	}

	/** Writes the file if something changed (called every tick, writes at most every 5 seconds). */
	public static synchronized void saveIfNeeded(boolean force) {
		if (!dirty || !force && System.currentTimeMillis() - dirtySince < 5000) {
			return;
		}
		dirty = false;
		JsonObject o = new JsonObject();
		JsonObject off = new JsonObject();
		OFFSETS.forEach(off::addProperty);
		o.add("offsets", off);
		JsonObject ch = new JsonObject();
		CHOICES.forEach(ch::addProperty);
		o.add("choices", ch);
		JsonArray hid = new JsonArray();
		HIDDEN.forEach(hid::add);
		o.add("hidden", hid);
		JsonArray rec = new JsonArray();
		for (Played p : RECENT) {
			JsonObject r = new JsonObject();
			r.addProperty("title", p.title());
			r.addProperty("artist", p.artist());
			r.addProperty("source", p.source());
			r.addProperty("at", p.at());
			rec.add(r);
		}
		o.add("recent", rec);
		JsonArray st = new JsonArray();
		for (Stat s : STATS.values()) {
			JsonObject r = new JsonObject();
			r.addProperty("title", s.title());
			r.addProperty("artist", s.artist());
			r.addProperty("plays", s.plays());
			r.addProperty("ms", s.listenedMs());
			r.addProperty("last", s.lastAt());
			st.add(r);
		}
		o.add("stats", st);
		Path f = file();
		try {
			Files.createDirectories(f.getParent());
			Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
			Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			try {
				Files.writeString(f, GSON.toJson(o), StandardCharsets.UTF_8);
			} catch (Exception ignored) {
				NovaClient.LOGGER.warn("Could not save {}", f, e);
			}
		}
	}

	// ------------------------------------------------------------------ lyrics per song

	/** Extra lyrics offset of this song in ms (positive = lyrics later). */
	public static synchronized long offset(String key) {
		ensureLoaded();
		return OFFSETS.getOrDefault(key, 0L);
	}

	public static synchronized void setOffset(String key, long ms) {
		ensureLoaded();
		ms = Math.max(-30_000, Math.min(30_000, ms));
		if (ms == 0) {
			OFFSETS.remove(key);
		} else {
			OFFSETS.put(key, ms);
		}
		changed();
	}

	/** LRCLIB id of the lyrics chosen for this song, or -1. */
	public static synchronized long choice(String key) {
		ensureLoaded();
		return CHOICES.getOrDefault(key, -1L);
	}

	public static synchronized void setChoice(String key, long id) {
		ensureLoaded();
		if (id < 0) {
			CHOICES.remove(key);
		} else {
			CHOICES.put(key, id);
		}
		changed();
	}

	/** True if the lyrics of this song are switched off. */
	public static synchronized boolean hidden(String key) {
		ensureLoaded();
		return HIDDEN.contains(key);
	}

	public static synchronized void setHidden(String key, boolean hidden) {
		ensureLoaded();
		if (hidden) {
			HIDDEN.add(key);
		} else {
			HIDDEN.remove(key);
		}
		changed();
	}

	// ------------------------------------------------------------------ history

	/** A song started playing. */
	public static synchronized void played(String title, String artist, String source) {
		ensureLoaded();
		RECENT.removeIf(p -> p.title().equals(title) && p.artist().equals(artist));
		RECENT.add(0, new Played(title, artist, source, System.currentTimeMillis()));
		while (RECENT.size() > MAX_RECENT) {
			RECENT.remove(RECENT.size() - 1);
		}
		changed();
	}

	/** Adds listening time; {@code countPlay}: the song has now been heard long enough to count as played. */
	public static synchronized void listened(String title, String artist, long ms, boolean countPlay) {
		ensureLoaded();
		String k = key(title, artist);
		Stat s = STATS.get(k);
		if (s == null) {
			s = new Stat(title, artist, 0, 0, 0);
		}
		STATS.put(k, new Stat(title, artist, s.plays() + (countPlay ? 1 : 0), s.listenedMs() + ms, System.currentTimeMillis()));
		if (STATS.size() > MAX_STATS) {
			// forget the songs heard the least
			STATS.values().stream().min(Comparator.comparingLong(Stat::listenedMs))
				.ifPresent(min -> STATS.remove(key(min.title(), min.artist())));
		}
		changed();
	}

	public static synchronized List<Played> recent() {
		ensureLoaded();
		return List.copyOf(RECENT);
	}

	/** Most played songs (plays, then listening time). */
	public static synchronized List<Stat> top(int n) {
		ensureLoaded();
		List<Stat> list = new ArrayList<>(STATS.values());
		list.removeIf(s -> s.plays() == 0);
		list.sort(Comparator.comparingInt(Stat::plays).reversed().thenComparing(Comparator.comparingLong(Stat::listenedMs).reversed()));
		return list.size() > n ? List.copyOf(list.subList(0, n)) : List.copyOf(list);
	}

	/** Total listening time of all songs in ms. */
	public static synchronized long totalListened() {
		ensureLoaded();
		long sum = 0;
		for (Stat s : STATS.values()) {
			sum += s.listenedMs();
		}
		return sum;
	}

	public static synchronized void clearHistory() {
		ensureLoaded();
		RECENT.clear();
		STATS.clear();
		changed();
	}
}
