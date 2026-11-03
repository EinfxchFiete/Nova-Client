package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.TextSetting;
import java.util.Locale;

/** Connection to the Nova server: lets Nova players see each other's cosmetics, songs and emotes. */
public class NetworkModule extends Module {
	/** Default Nova server (Render.com, see backend/README.md). */
	public static final String DEFAULT_SERVER = "https://nova-server-ifqq.onrender.com";

	private static NetworkModule instance;

	public final TextSetting server = add(new TextSetting("Server-Adresse", DEFAULT_SERVER, 120) {
		@Override
		public void fromJson(com.google.gson.JsonElement element) {
			super.fromJson(element);
			// configs from before 1.4.1 stored an empty address; use the official server instead
			if (get().isBlank()) {
				set(DEFAULT_SERVER);
			}
		}
	});
	public final BoolSetting showOthers = add(new BoolSetting("Cosmetics anderer anzeigen", true));
	public final BoolSetting showBadges = add(new BoolSetting("Nova-Symbol anderer anzeigen", true));

	public NetworkModule() {
		super("Nova-Netzwerk", "Andere Nova-Spieler sehen deine Cosmetics, Emotes und Songs", Category.UTILITY, true);
		instance = this;
	}

	public static NetworkModule get() {
		return instance;
	}

	/**
	 * Normalised server URL or "" if none/invalid. Only https is allowed, except for local test servers.
	 */
	public String serverUrl() {
		String s = server.get().trim();
		while (s.endsWith("/")) {
			s = s.substring(0, s.length() - 1);
		}
		if (s.isEmpty()) {
			return "";
		}
		String lower = s.toLowerCase(Locale.ROOT);
		if (lower.startsWith("https://")) {
			return s;
		}
		if (lower.startsWith("http://127.0.0.1") || lower.startsWith("http://localhost")) {
			return s;
		}
		if (!lower.contains("://")) {
			return "https://" + s;
		}
		return "";
	}
}
