package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;

/** Dropped items lie flat on the ground instead of floating and spinning. */
public class ItemPhysicsModule extends Module {
	private static ItemPhysicsModule instance;

	public ItemPhysicsModule() {
		super("Item-Physik", "Gegenstände auf dem Boden liegen flach, statt zu schweben und sich zu drehen", Category.VISUAL, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
