package dev.novaclient.module.cosmetics;

import dev.novaclient.cosmetics.CosmeticRegistry;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;

/** Animated 3D wings on your back. */
public class WingsModule extends Module {
	private static WingsModule instance;

	public final ModeSetting type = add(new ModeSetting("Flügel", "Engelsflügel", CosmeticRegistry.WINGS.values().toArray(new String[0])));
	public final ModeSetting color = add(new ModeSetting("Farbe", "Normal", CosmeticRegistry.COLORS.keySet().toArray(new String[0])));
	public final dev.novaclient.setting.BoolSetting particles = add(new dev.novaclient.setting.BoolSetting("Partikel (Phönix, Fee, Eis)", true));

	public WingsModule() {
		super("Flügel", "Animierte Engels-, Drachen- oder Schmetterlingsflügel", Category.COSMETICS, false);
		instance = this;
	}

	public static WingsModule get() {
		return instance;
	}

	public String currentId() {
		return CosmeticRegistry.idForName(CosmeticRegistry.WINGS, type.get());
	}
}
