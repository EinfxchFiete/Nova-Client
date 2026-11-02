package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;

public class FullbrightModule extends Module {
	private static FullbrightModule instance;

	public FullbrightModule() {
		super("Fullbright", "Alles ist hell, auch in Höhlen und nachts", Category.VISUAL, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
