package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import net.minecraft.resources.Identifier;

/** Hides annoying full-screen overlays. */
public class OverlaysModule extends Module {
	private static OverlaysModule instance;

	public final BoolSetting pumpkin = add(new BoolSetting("Kürbis-Overlay ausblenden", true));
	public final BoolSetting powderSnow = add(new BoolSetting("Pulverschnee-Overlay ausblenden", true));
	public final BoolSetting vignette = add(new BoolSetting("Vignette ausblenden", false));
	public final BoolSetting water = add(new BoolSetting("Unterwasser-Overlay ausblenden", false));

	public OverlaysModule() {
		super("Overlays", "Blendet Kürbis-, Pulverschnee-, Unterwasser-Overlay und Vignette aus", Category.VISUAL, false);
		instance = this;
	}

	public static boolean hideTexture(Identifier texture) {
		OverlaysModule o = instance;
		if (o == null || !o.isEnabled() || texture == null) {
			return false;
		}
		String path = texture.getPath();
		return o.pumpkin.isOn() && path.contains("pumpkinblur") || o.powderSnow.isOn() && path.contains("powder_snow_outline");
	}

	public static boolean hideWater() {
		return instance != null && instance.isEnabled() && instance.water.isOn();
	}

	public static boolean hideVignette() {
		return instance != null && instance.isEnabled() && instance.vignette.isOn();
	}
}
