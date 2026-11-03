package dev.novaclient.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novaclient.NovaClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Counts how long you played in every world and on every server. Saved in config/novaclient/playtime.json,
 * so the time continues after leaving and joining again.
 */
public final class Playtime {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("novaclient").resolve("playtime.json");
	private static final long SAVE_EVERY_MS = 30_000;

	private static final Map<String, Long> TOTAL = new LinkedHashMap<>();
	private static boolean loaded;
	private static String currentKey;
	private static long lastTick;
	private static long sessionMs;
	private static long lastSave;
	private static boolean dirty;

	private Playtime() {
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		try {
			if (Files.exists(FILE)) {
				JsonObject o = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
				for (var e : o.entrySet()) {
					if (e.getValue().isJsonPrimitive()) {
						TOTAL.put(e.getKey(), Math.max(0, e.getValue().getAsLong()));
					}
				}
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not read playtime.json", e);
		}
	}

	public static synchronized void save() {
		if (!dirty) {
			return;
		}
		dirty = false;
		JsonObject o = new JsonObject();
		TOTAL.forEach(o::addProperty);
		try {
			Files.createDirectories(FILE.getParent());
			Path tmp = FILE.resolveSibling("playtime.json.tmp");
			Files.writeString(tmp, o.toString(), StandardCharsets.UTF_8);
			Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			NovaClient.LOGGER.warn("Could not save playtime.json", e);
		}
	}

	/** Called every client tick. */
	public static synchronized void tick(Minecraft mc) {
		load();
		long now = System.currentTimeMillis();
		String key = WorldId.key(mc);
		if (key == null || !key.equals(currentKey)) {
			if (currentKey != null) {
				save();
			}
			currentKey = key;
			sessionMs = 0;
			lastTick = now;
			return;
		}
		long dt = now - lastTick;
		lastTick = now;
		// a paused singleplayer game does not count, long hangs (e.g. sleeping PC) neither
		if (dt <= 0 || dt > 5000 || mc.isPaused()) {
			return;
		}
		TOTAL.merge(key, dt, Long::sum);
		sessionMs += dt;
		dirty = true;
		if (now - lastSave > SAVE_EVERY_MS) {
			lastSave = now;
			save();
		}
	}

	/** Total play time in the current world in ms (0 when not in a world). */
	public static synchronized long currentTotal() {
		load();
		return currentKey == null ? 0 : TOTAL.getOrDefault(currentKey, 0L);
	}

	/** Play time since you joined the current world. */
	public static synchronized long currentSession() {
		return currentKey == null ? 0 : sessionMs;
	}

	/** All worlds, longest first. */
	public static synchronized List<Map.Entry<String, Long>> all() {
		load();
		List<Map.Entry<String, Long>> list = new ArrayList<>(TOTAL.entrySet());
		list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
		return list;
	}

	/** For tests: sets the stored time of a world. */
	public static synchronized void setForTest(String key, long ms) {
		load();
		TOTAL.put(key, ms);
		dirty = true;
	}

	/** "3 Std. 12 Min." / "12 Min. 5 Sek." */
	public static String format(long ms) {
		long s = ms / 1000;
		long d = s / 86400;
		long h = (s % 86400) / 3600;
		long m = (s % 3600) / 60;
		long sec = s % 60;
		if (d > 0) {
			return d + " Tg. " + h + " Std. " + m + " Min.";
		}
		if (h > 0) {
			return h + " Std. " + m + " Min.";
		}
		return m + " Min. " + sec + " Sek.";
	}
}
