package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.TextSetting;
import java.util.Locale;

/** Connection to the Nova server: lets Nova players see each other's cosmetics, songs and emotes. */
public class NetworkModule extends Module {
	/** Default Nova server; set after the server has been deployed (see backend/README.md). */
	public static final String DEFAULT_SERVER = "";

	private static NetworkModule instance;

	public final TextSetting server = add(new TextSetting("Server-Adresse", DEFAULT_SERVER, 120));
	public final BoolSetting showOthers = add(new BoolSetting("Cosmetics anderer anzeigen", true));
	public final BoolSetting showBadges = add(new BoolSetting("Nova-Symbol anderer anzeigen", true));
	public final BoolSetting chatBadges = add(new BoolSetting("Abzeichen im Chat", true));

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
