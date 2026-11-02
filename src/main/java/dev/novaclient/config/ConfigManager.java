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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file = FabricLoader.getInstance().getConfigDir().resolve("novaclient.json");
	private final ModuleManager modules;

	public ConfigManager(ModuleManager modules) {
		this.modules = modules;
	}

	public void load() {
		if (!Files.isRegularFile(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement rootEl = JsonParser.parseReader(reader);
			if (!rootEl.isJsonObject()) {
				return;
			}
			JsonObject root = rootEl.getAsJsonObject();
			if (!root.has("modules") || !root.get("modules").isJsonObject()) {
				return;
			}
			JsonObject mods = root.getAsJsonObject("modules");
			for (Module m : modules.getModules()) {
				if (!mods.has(m.getName()) || !mods.get(m.getName()).isJsonObject()) {
					continue;
				}
				try {
					applyModule(m, mods.getAsJsonObject(m.getName()));
				} catch (RuntimeException e) {
					NovaClient.LOGGER.warn("Ignoring broken config entry for {}", m.getName(), e);
				}
			}
		} catch (IOException | RuntimeException e) {
			NovaClient.LOGGER.error("Could not read {}, using defaults", file, e);
		}
	}

	private static void applyModule(Module m, JsonObject obj) {
		if (obj.has("enabled") && obj.get("enabled").isJsonPrimitive()) {
			m.loadEnabled(obj.get("enabled").getAsBoolean());
		}
		if (obj.has("key") && obj.get("key").isJsonPrimitive()) {
			m.setKeybind(obj.get("key").getAsInt());
		}
		if (m instanceof HudModule h && obj.has("anchorX") && obj.has("anchorY") && obj.has("x") && obj.has("y")) {
			h.setPosition(obj.get("anchorX").getAsString(), obj.get("x").getAsInt(), obj.get("anchorY").getAsString(), obj.get("y").getAsInt());
		}
		if (obj.has("settings") && obj.get("settings").isJsonObject()) {
			JsonObject settings = obj.getAsJsonObject("settings");
			for (Setting<?> s : m.getSettings()) {
				if (settings.has(s.getName())) {
					s.fromJson(settings.get(s.getName()));
				}
			}
		}
	}

	public void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
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
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling("novaclient.json.tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			NovaClient.LOGGER.error("Could not save {}", file, e);
		}
	}
}
