package dev.novaclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novaclient.NovaClient;
import dev.novaclient.module.Module;
import dev.novaclient.module.ModuleManager;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.setting.Setting;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Stores all module settings in profiles: config/novaclient/profiles/NAME.json.
 * The active profile name is kept in config/novaclient/client.json.
 */
public final class ConfigManager {
	public static final String DEFAULT_PROFILE = "Standard";
	public static final String[] PRESETS = {"PvP", "Survival", "Minimal"};
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("novaclient");
	private final Path profilesDir = dir.resolve("profiles");
	private final Path metaFile = dir.resolve("client.json");
	private final Path legacyFile = FabricLoader.getInstance().getConfigDir().resolve("novaclient.json");
	private final ModuleManager modules;
	private String active = DEFAULT_PROFILE;

	public ConfigManager(ModuleManager modules) {
		this.modules = modules;
	}

	// ------------------------------------------------------------------ profiles

	public String getActiveProfile() {
		return active;
	}

	public List<String> listProfiles() {
		List<String> out = new ArrayList<>();
		if (Files.isDirectory(profilesDir)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.json")) {
				for (Path p : stream) {
					String name = p.getFileName().toString();
					out.add(name.substring(0, name.length() - ".json".length()));
				}
			} catch (IOException e) {
				NovaClient.LOGGER.error("Could not list profiles", e);
			}
		}
		if (!out.contains(active)) {
			out.add(active);
		}
		Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
		return out;
	}

	/** Removes characters that are not allowed in file names. Returns "" if nothing is left. */
	public static String sanitize(String name) {
		String cleaned = name.replaceAll("[^A-Za-z0-9ÄÖÜäöüß _-]", "").trim();
		return cleaned.length() > 24 ? cleaned.substring(0, 24).trim() : cleaned;
	}

	private Path profileFile(String name) {
		return profilesDir.resolve(name + ".json");
	}

	public boolean exists(String name) {
		return Files.isRegularFile(profileFile(name)) || name.equals(active);
	}

	/** A free name based on the wish, e.g. "PvP 2". */
	public String freeName(String wish) {
		String base = sanitize(wish);
		if (base.isEmpty()) {
			base = "Profil";
		}
		if (!exists(base)) {
			return base;
		}
		for (int i = 2; ; i++) {
			String candidate = sanitize(base + " " + i);
			if (!exists(candidate)) {
				return candidate;
			}
		}
	}

	/** Saves the current profile and switches to another one (applying it live). */
	public void switchTo(String name) {
		if (name.equals(active) || !Files.isRegularFile(profileFile(name))) {
			return;
		}
		save();
		active = name;
		for (Module m : modules.getModules()) {
			m.resetToDefaults();
		}
		readProfile(profileFile(name), true);
		saveMeta();
	}

	/** Creates a new profile as a copy of the current settings and activates it. */
	public String createCopy(String wish) {
		String name = freeName(wish);
		save();
		active = name;
		save();
		saveMeta();
		return name;
	}

	/** Creates a profile from a built-in preset and activates it. */
	public String createFromPreset(String preset) {
		String name = freeName(preset);
		save();
		active = name;
		for (Module m : modules.getModules()) {
			m.resetToDefaults();
		}
		Presets.apply(preset, modules);
		save();
		saveMeta();
		return name;
	}

	/** Deletes a profile that is not active. */
	public boolean delete(String name) {
		if (name.equals(active)) {
			return false;
		}
		try {
			return Files.deleteIfExists(profileFile(name));
		} catch (IOException e) {
			NovaClient.LOGGER.error("Could not delete profile {}", name, e);
			return false;
		}
	}

	// ------------------------------------------------------------------ load / save

	/** Called once at start-up (before the game is fully initialised: no side effects). */
	public void load() {
		try {
			Files.createDirectories(profilesDir);
			if (Files.isRegularFile(legacyFile) && !Files.exists(profileFile(DEFAULT_PROFILE))) {
				Files.move(legacyFile, profileFile(DEFAULT_PROFILE));
				NovaClient.LOGGER.info("Migrated old config to profile '{}'", DEFAULT_PROFILE);
			}
		} catch (IOException e) {
			NovaClient.LOGGER.error("Could not prepare config folder", e);
		}
		active = readMeta();
		if (!Files.isRegularFile(profileFile(active))) {
			active = DEFAULT_PROFILE;
		}
		readProfile(profileFile(active), false);
	}

	/** Re-reads the active profile (used by tests). */
	public void reload() {
		readProfile(profileFile(active), true);
	}

	private String readMeta() {
		if (!Files.isRegularFile(metaFile)) {
			return DEFAULT_PROFILE;
		}
		try (Reader reader = Files.newBufferedReader(metaFile, StandardCharsets.UTF_8)) {
			JsonElement el = JsonParser.parseReader(reader);
			if (el.isJsonObject() && el.getAsJsonObject().has("activeProfile")) {
				String name = sanitize(el.getAsJsonObject().get("activeProfile").getAsString());
				return name.isEmpty() ? DEFAULT_PROFILE : name;
			}
		} catch (IOException | RuntimeException e) {
			NovaClient.LOGGER.warn("Could not read {}", metaFile, e);
		}
		return DEFAULT_PROFILE;
	}

	private void saveMeta() {
		JsonObject root = new JsonObject();
		root.addProperty("activeProfile", active);
		write(metaFile, root);
	}

	private void readProfile(Path file, boolean runtime) {
		if (!Files.isRegularFile(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement rootEl = JsonParser.parseReader(reader);
			if (!rootEl.isJsonObject() || !rootEl.getAsJsonObject().has("modules")
				|| !rootEl.getAsJsonObject().get("modules").isJsonObject()) {
				return;
			}
			JsonObject mods = rootEl.getAsJsonObject().getAsJsonObject("modules");
			for (Module m : modules.getModules()) {
				if (!mods.has(m.getName()) || !mods.get(m.getName()).isJsonObject()) {
					continue;
				}
				try {
					applyModule(m, mods.getAsJsonObject(m.getName()), runtime);
				} catch (RuntimeException e) {
					NovaClient.LOGGER.warn("Ignoring broken config entry for {}", m.getName(), e);
				}
			}
		} catch (IOException | RuntimeException e) {
			NovaClient.LOGGER.error("Could not read {}, using defaults", file, e);
		}
	}

	private static void applyModule(Module m, JsonObject obj, boolean runtime) {
		if (obj.has("settings") && obj.get("settings").isJsonObject()) {
			JsonObject settings = obj.getAsJsonObject("settings");
			for (Setting<?> s : m.getSettings()) {
				if (settings.has(s.getName())) {
					s.fromJson(settings.get(s.getName()));
				}
			}
		}
		if (obj.has("key") && obj.get("key").isJsonPrimitive()) {
			m.setKeybind(obj.get("key").getAsInt());
		}
		if (m instanceof HudModule h && obj.has("anchorX") && obj.has("anchorY") && obj.has("x") && obj.has("y")) {
			h.setPosition(obj.get("anchorX").getAsString(), obj.get("x").getAsInt(), obj.get("anchorY").getAsString(), obj.get("y").getAsInt());
		}
		if (obj.has("enabled") && obj.get("enabled").isJsonPrimitive()) {
			if (runtime) {
				m.setEnabled(obj.get("enabled").getAsBoolean());
			} else {
				m.loadEnabled(obj.get("enabled").getAsBoolean());
			}
		}
	}

	public void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 2);
		JsonObject mods = new JsonObject();
		for (Module m : modules.getModules()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("enabled", m.isEnabled());
			obj.addProperty("key", m.getKeybind());
			if (m instanceof HudModule h) {
				obj.addProperty("anchorX", h.getAnchorX().name());
				obj.addProperty("x", h.getOffsetX());
				obj.addProperty("anchorY", h.getAnchorY().name());
				obj.addProperty("y", h.getOffsetY());
			}
			JsonObject settings = new JsonObject();
			for (Setting<?> s : m.getSettings()) {
				settings.add(s.getName(), s.toJson());
			}
			obj.add("settings", settings);
			mods.add(m.getName(), obj);
		}
		root.add("modules", mods);
		write(profileFile(active), root);
		saveMeta();
	}

	private static void write(Path file, JsonObject root) {
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			NovaClient.LOGGER.error("Could not save {}", file, e);
		}
	}

	/** Built-in profile templates. */
	static final class Presets {
		private static final Set<String> PVP = Set.of("Theme", "FPS", "CPS", "Keystrokes", "Rüstungsstatus", "Trankeffekte",
			"Reichweite", "Combo", "Ziel-HUD", "Zoom", "Freelook", "Custom Crosshair", "No Hurt Cam", "Low Fire",
			"Toggle Sprint", "Treffer-Partikel", "Benachrichtigungen", "Haltbarkeitswarnung", "Ping");
		private static final Set<String> SURVIVAL = Set.of("Theme", "FPS", "Koordinaten", "Biom", "Tag-Zähler", "Uhrzeit",
			"Lichtlevel", "Kompassleiste", "Rüstungsstatus", "Trankeffekte", "Zoom", "Freelook", "Fullbright",
			"Todes-Koordinaten", "Haltbarkeitswarnung", "Benachrichtigungen", "Overlays");
		private static final Set<String> MINIMAL = Set.of("Theme", "FPS", "Zoom", "Freelook", "Benachrichtigungen");

		private Presets() {
		}

		static void apply(String preset, ModuleManager modules) {
			Set<String> on = switch (preset) {
				case "PvP" -> PVP;
				case "Survival" -> SURVIVAL;
				default -> MINIMAL;
			};
			for (Module m : modules.getModules()) {
				m.setEnabled(on.contains(m.getName()));
			}
		}
	}
}
