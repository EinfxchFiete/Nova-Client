package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;

/** Repeated chat messages are merged into one line with a counter ("(x3)"). */
public class CompactChatModule extends Module {
	private static CompactChatModule instance;

	public CompactChatModule() {
		super("Kompakter Chat", "Gleiche Nachrichten hintereinander werden zusammengefasst (x2, x3 …)", Category.UTILITY, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
