package dev.novaclient.world;

import com.google.gson.JsonArray;
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
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Waypoints per world/server and dimension, saved in config/novaclient/waypoints.json
 * (before 1.5.0 there was only one waypoint that was lost on restart and showed up on every server).
 */
public final class Waypoints {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("novaclient").resolve("waypoints.json");
	public static final int MAX_PER_WORLD = 50;
	private static final int[] COLORS = {0xFF8B5CF6, 0xFF22D3EE, 0xFF4ADE80, 0xFFFACC15, 0xFFF97316, 0xFFF43F5E, 0xFFEC4899, 0xFFFFFFFF};

	public record Waypoint(String name, String dimension, int x, int y, int z, int color) {
		public double distanceTo(LocalPlayer p) {
			double dx = x + 0.5 - p.getX();
			double dy = y - p.getY();
			double dz = z + 0.5 - p.getZ();
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
	}

	private static final Map<String, List<Waypoint>> ALL = new LinkedHashMap<>();
	private static boolean loaded;

	private Waypoints() {
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		try {
			if (!Files.exists(FILE)) {
				return;
			}
			JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
			for (var e : root.entrySet()) {
				List<Waypoint> list = new ArrayList<>();
				for (var el : e.getValue().getAsJsonArray()) {
					JsonObject o = el.getAsJsonObject();
					list.add(new Waypoint(o.get("name").getAsString(), o.get("dim").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(),
						o.get("z").getAsInt(), o.has("color") ? o.get("color").getAsInt() : COLORS[0]));
				}
				ALL.put(e.getKey(), list);
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not read waypoints.json", e);
		}
	}

	private static void save() {
		JsonObject root = new JsonObject();
		for (var e : ALL.entrySet()) {
			if (e.getValue().isEmpty()) {
				continue;
			}
			JsonArray arr = new JsonArray();
			for (Waypoint w : e.getValue()) {
				JsonObject o = new JsonObject();
				o.addProperty("name", w.name());
				o.addProperty("dim", w.dimension());
				o.addProperty("x", w.x());
				o.addProperty("y", w.y());
				o.addProperty("z", w.z());
				o.addProperty("color", w.color());
				arr.add(o);
			}
			root.add(e.getKey(), arr);
		}
		try {
			Files.createDirectories(FILE.getParent());
			Path tmp = FILE.resolveSibling("waypoints.json.tmp");
			Files.writeString(tmp, root.toString(), StandardCharsets.UTF_8);
			Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			NovaClient.LOGGER.warn("Could not save waypoints.json", e);
		}
	}

	private static String dimension(LocalPlayer p) {
		return p.level().dimension().identifier().toString();
	}

	/** Waypoints of the current world (all dimensions). */
	public static synchronized List<Waypoint> inWorld(Minecraft mc) {
		load();
		String key = WorldId.key(mc);
		return key == null ? List.of() : List.copyOf(ALL.getOrDefault(key, List.of()));
	}

	/** Waypoints of the current world in your dimension. */
	public static List<Waypoint> here(Minecraft mc) {
		if (mc.player == null) {
			return List.of();
		}
		String dim = dimension(mc.player);
		return inWorld(mc).stream().filter(w -> w.dimension().equals(dim)).toList();
	}

	public static @Nullable Waypoint nearest(Minecraft mc) {
		Waypoint best = null;
		double bestD = Double.MAX_VALUE;
		for (Waypoint w : here(mc)) {
			double d = w.distanceTo(mc.player);
			if (d < bestD) {
				bestD = d;
				best = w;
			}
		}
		return best;
	}

	/** Adds a waypoint at your position; an existing one with the same name is replaced. Returns null if not in a world. */
	public static synchronized @Nullable Waypoint add(Minecraft mc, @Nullable String name) {
		load();
		String key = WorldId.key(mc);
		LocalPlayer p = mc.player;
		if (key == null || p == null) {
			return null;
		}
		List<Waypoint> list = ALL.computeIfAbsent(key, k -> new ArrayList<>());
		String n = name == null || name.isBlank() ? nextName(list) : name.trim();
		if (n.length() > 32) {
			n = n.substring(0, 32);
		}
		final String fn = n;
		list.removeIf(w -> w.name().equalsIgnoreCase(fn));
		if (list.size() >= MAX_PER_WORLD) {
			list.remove(0);
		}
		Waypoint w = new Waypoint(n, dimension(p), p.getBlockX(), p.getBlockY(), p.getBlockZ(), COLORS[list.size() % COLORS.length]);
		list.add(w);
		save();
		return w;
	}

	private static String nextName(List<Waypoint> list) {
		for (int i = 1; ; i++) {
			String n = "Wegpunkt " + i;
			if (list.stream().noneMatch(w -> w.name().equalsIgnoreCase(n))) {
				return n;
			}
		}
	}

	/** Removes a waypoint of the current world by name (case-insensitive). */
	public static synchronized boolean remove(Minecraft mc, String name) {
		load();
		String key = WorldId.key(mc);
		if (key == null) {
			return false;
		}
		List<Waypoint> list = ALL.get(key);
		boolean removed = list != null && list.removeIf(w -> w.name().equalsIgnoreCase(name.trim()));
		if (removed) {
			save();
		}
		return removed;
	}

	/** Removes all waypoints of the current world, returns how many. */
	public static synchronized int clear(Minecraft mc) {
		load();
		String key = WorldId.key(mc);
		List<Waypoint> list = key == null ? null : ALL.remove(key);
		save();
		return list == null ? 0 : list.size();
	}
}
