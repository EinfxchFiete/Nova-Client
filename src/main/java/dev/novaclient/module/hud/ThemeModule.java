package dev.novaclient.module.hud;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.NumberSetting;
import java.awt.Color;

/** Accent colour used by the HUD and the menu. Configurable on the "Design" page. */
public class ThemeModule extends Module {
	private static ThemeModule instance;

	/** Name, hue (0-360), saturation, brightness of the colour presets on the design page. */
	public record Preset(String name, double hue, double saturation, double brightness) {
		public int color() {
			return 0xFF000000 | (Color.HSBtoRGB((float) (hue / 360.0), (float) saturation, (float) brightness) & 0xFFFFFF);
		}
	}

	public static final Preset[] PRESETS = {
		new Preset("Lila", 265, 0.65, 1.0),
		new Preset("Blau", 220, 0.75, 1.0),
		new Preset("Cyan", 188, 0.75, 1.0),
		new Preset("Mint", 155, 0.55, 0.95),
		new Preset("Grün", 120, 0.65, 0.9),
		new Preset("Gelb", 52, 0.75, 1.0),
		new Preset("Orange", 28, 0.8, 1.0),
		new Preset("Rot", 0, 0.75, 1.0),
		new Preset("Pink", 320, 0.55, 1.0),
		new Preset("Rosa", 345, 0.35, 1.0),
		new Preset("Gold", 42, 0.65, 0.85),
		new Preset("Weiß", 0, 0.0, 1.0),
	};

	public final NumberSetting hue = add(new NumberSetting("Farbton", 265, 0, 360, 1));
	public final NumberSetting saturation = add(new NumberSetting("Sättigung", 0.65, 0, 1, 0.05));
	public final NumberSetting brightness = add(new NumberSetting("Helligkeit", 1.0, 0.3, 1, 0.05));
	public final BoolSetting rainbow = add(new BoolSetting("Regenbogen", false));
	public final NumberSetting rainbowSpeed = add(new NumberSetting("Regenbogen-Tempo", 4, 1, 10, 1));
	public final NumberSetting menuOpacity = add(new NumberSetting("Menü-Deckkraft", 0.94, 0.6, 1.0, 0.02));

	public ThemeModule() {
		super("Theme", "Akzentfarbe für HUD und Menü (siehe Design)", Category.HUD, true);
		instance = this;
	}

	public static ThemeModule get() {
		return instance;
	}

	public void apply(Preset p) {
		hue.set(p.hue());
		saturation.set(p.saturation());
		brightness.set(p.brightness());
		rainbow.set(false);
	}

	public boolean matches(Preset p) {
		return !rainbow.isOn() && Math.abs(hue.get() - p.hue()) < 0.5 && Math.abs(saturation.get() - p.saturation()) < 0.01
			&& Math.abs(brightness.get() - p.brightness()) < 0.01;
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
		return 0xFF000000 | (Color.HSBtoRGB(h, t.saturation.getFloat(), t.brightness.getFloat()) & 0xFFFFFF);
	}

	/** Alpha (0-255) for menu backgrounds. */
	public static int menuAlpha() {
		ThemeModule t = instance;
		return t == null ? 240 : (int) Math.round(t.menuOpacity.get() * 255);
	}
}
