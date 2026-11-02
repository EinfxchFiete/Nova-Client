package dev.novaclient.module.hud;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.NumberSetting;
import java.awt.Color;

/** Accent colour used by the HUD and the menu. */
public class ThemeModule extends Module {
	private static ThemeModule instance;

	public final NumberSetting hue = add(new NumberSetting("Farbton", 265, 0, 360, 1));
	public final NumberSetting saturation = add(new NumberSetting("Sättigung", 0.65, 0, 1, 0.05));
	public final BoolSetting rainbow = add(new BoolSetting("Regenbogen", false));
	public final NumberSetting rainbowSpeed = add(new NumberSetting("Regenbogen-Tempo", 4, 1, 10, 1));

	public ThemeModule() {
		super("Theme", "Akzentfarbe für HUD und Menü", Category.HUD, true);
		instance = this;
	}

	/** Opaque ARGB accent colour. Falls back to white when the module is off. */
	public static int accent() {
		return accent(0);
	}

	public static int accent(int offset) {
		ThemeModule t = instance;
		if (t == null || !t.isEnabled()) {
			return 0xFFFFFFFF;
		}
		float h;
		if (t.rainbow.isOn()) {
			double period = 11000.0 / t.rainbowSpeed.get();
			h = (float) (((System.currentTimeMillis() + offset * 120L) % (long) period) / period);
		} else {
			h = (float) (t.hue.get() / 360.0);
		}
		return 0xFF000000 | (Color.HSBtoRGB(h, t.saturation.getFloat(), 1.0F) & 0xFFFFFF);
	}

	@Override
	protected void onDisable() {
		// Theme is allowed to be disabled; accent() falls back to white.
	}
}
