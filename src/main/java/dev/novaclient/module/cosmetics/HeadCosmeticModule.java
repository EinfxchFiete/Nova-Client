package dev.novaclient.module.cosmetics;

import dev.novaclient.cosmetics.CosmeticRegistry;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;

/** 3D head cosmetic: bandana, crown, top hat, horns, cat ears or halo. */
public class HeadCosmeticModule extends Module {
	private static HeadCosmeticModule instance;

	public final ModeSetting type = add(new ModeSetting("Kopfschmuck", "Bandana", CosmeticRegistry.HEADS.values().toArray(new String[0])));
	public final ModeSetting color = add(new ModeSetting("Farbe", "Normal", CosmeticRegistry.COLORS.keySet().toArray(new String[0])));

	public HeadCosmeticModule() {
		super("Kopfschmuck", "3D-Bandana, Krone, Zylinder, Hörner, Katzenohren oder Heiligenschein", Category.COSMETICS, false);
		instance = this;
	}

	public static HeadCosmeticModule get() {
		return instance;
	}

	public String currentId() {
		return CosmeticRegistry.idForName(CosmeticRegistry.HEADS, type.get());
	}
}
