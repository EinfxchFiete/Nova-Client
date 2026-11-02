package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;

public class NoHurtCamModule extends Module {
	private static NoHurtCamModule instance;

	public NoHurtCamModule() {
		super("No Hurt Cam", "Entfernt das Kamerawackeln bei Schaden", Category.VISUAL, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
