package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;

/** Keeps more chat messages (Minecraft keeps only 100). */
public class LongChatModule extends Module {
	private static LongChatModule instance;

	public final NumberSetting lines = add(new NumberSetting("Nachrichten", 500, 100, 1000, 50));

	public LongChatModule() {
		super("Langer Chat-Verlauf", "Speichert bis zu 1000 statt 100 Chat-Nachrichten zum Zurückscrollen", Category.UTILITY, false);
		instance = this;
	}

	/** Maximum number of messages; 100 when off. */
	public static int limit(int vanilla) {
		return instance != null && instance.isEnabled() ? Math.max(vanilla, instance.lines.getInt()) : vanilla;
	}
}
