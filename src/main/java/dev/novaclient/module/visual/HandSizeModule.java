package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;

/** Size and position of your hands and items in first person. */
public class HandSizeModule extends Module {
	private static HandSizeModule instance;

	public final NumberSetting size = add(new NumberSetting("Größe", 0.8, 0.3, 1.5, 0.05));
	public final NumberSetting offsetX = add(new NumberSetting("Seitlich", 0, -0.5, 0.5, 0.05));
	public final NumberSetting offsetY = add(new NumberSetting("Höhe", 0, -0.5, 0.5, 0.05));

	public HandSizeModule() {
		super("Hand-Größe", "Hände und Gegenstände in der 1. Person kleiner, größer oder verschoben", Category.VISUAL, false);
		instance = this;
	}

	public static HandSizeModule active() {
		return instance != null && instance.isEnabled() ? instance : null;
	}
}
