package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;

/** Colour and thickness of the outline around the block you look at. */
public class BlockOutlineModule extends Module {
	private static BlockOutlineModule instance;

	public final ModeSetting color = add(new ModeSetting("Farbe", "Theme", "Theme", "Weiß", "Schwarz", "Rot", "Grün", "Blau", "Gelb", "Pink", "Cyan"));
	public final NumberSetting opacity = add(new NumberSetting("Deckkraft", 0.8, 0.1, 1.0, 0.05));
	public final NumberSetting thickness = add(new NumberSetting("Dicke", 2.0, 1.0, 6.0, 0.5));

	public BlockOutlineModule() {
		super("Blockumrandung", "Farbe und Dicke der Umrandung des anvisierten Blocks", Category.VISUAL, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	public static int color(int original) {
		BlockOutlineModule b = instance;
		int rgb = switch (b.color.get()) {
			case "Theme" -> ThemeModule.accent() & 0xFFFFFF;
			case "Weiß" -> 0xFFFFFF;
			case "Rot" -> 0xFF4040;
			case "Grün" -> 0x40FF60;
			case "Blau" -> 0x4080FF;
			case "Gelb" -> 0xFFE040;
			case "Pink" -> 0xFF60D0;
			case "Cyan" -> 0x40E8FF;
			default -> 0x000000;
		};
		return ((int) Math.round(b.opacity.get() * 255) << 24) | rgb;
	}

	public static float width(float original) {
		return original * instance.thickness.getFloat();
	}
}
