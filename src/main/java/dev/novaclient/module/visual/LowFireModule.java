package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;

public class LowFireModule extends Module {
	private static LowFireModule instance;

	public final NumberSetting offset = add(new NumberSetting("Absenkung", 0.2, 0.05, 0.4, 0.05));

	public LowFireModule() {
		super("Low Fire", "Senkt das Feuer-Overlay ab, damit du mehr siehst", Category.VISUAL, false);
		instance = this;
	}

	public static float activeOffset() {
		return instance != null && instance.isEnabled() ? instance.offset.getFloat() : 0.0F;
	}
}
