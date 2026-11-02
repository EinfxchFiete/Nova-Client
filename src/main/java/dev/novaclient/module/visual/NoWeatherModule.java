package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;

/** Client-side only: hides rain and thunderstorms. */
public class NoWeatherModule extends Module {
	private static NoWeatherModule instance;

	public NoWeatherModule() {
		super("Kein Regen", "Blendet Regen und Gewitter aus (nur bei dir)", Category.VISUAL, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
