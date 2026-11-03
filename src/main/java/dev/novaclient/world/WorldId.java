package dev.novaclient.world;

import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/** Identifies the world you are playing in: a singleplayer save folder or a server address. */
public final class WorldId {
	private WorldId() {
	}

	/** Stable key, e.g. "sp:New World" or "mp:play.example.net"; null when not in a world. */
	public static @Nullable String key(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			return null;
		}
		IntegratedServer sp = mc.getSingleplayerServer();
		if (sp != null) {
			Path root = sp.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
			Path name = root.getFileName();
			return "sp:" + (name == null ? sp.getWorldData().getLevelName() : name.toString());
		}
		ServerData server = mc.getCurrentServer();
		if (server != null) {
			String ip = server.ip.trim().toLowerCase(Locale.ROOT);
			if (ip.endsWith(":25565")) {
				ip = ip.substring(0, ip.length() - 6);
			}
			return "mp:" + ip;
		}
		return "mp:unknown";
	}

	/** Human readable name of the current world/server. */
	public static String displayName(Minecraft mc) {
		IntegratedServer sp = mc.getSingleplayerServer();
		if (sp != null) {
			return sp.getWorldData().getLevelName();
		}
		ServerData server = mc.getCurrentServer();
		return server != null ? server.ip : "Server";
	}

	/** Display name for a stored key. */
	public static String nameOfKey(String key) {
		return key.length() > 3 ? key.substring(3) : key;
	}
}
