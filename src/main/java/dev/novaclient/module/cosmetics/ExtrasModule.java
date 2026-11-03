package dev.novaclient.module.cosmetics;

import dev.novaclient.cosmetics.CosmeticRegistry;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;

/** Body extras: backpack, cat/fox/devil tail or scarf (3D, animated tails). */
public class ExtrasModule extends Module {
	private static ExtrasModule instance;

	public final ModeSetting type = add(new ModeSetting("Extra", "Rucksack", CosmeticRegistry.EXTRAS.values().toArray(new String[0])));
	public final ModeSetting color = add(new ModeSetting("Farbe", "Normal", CosmeticRegistry.COLORS.keySet().toArray(new String[0])));

	public ExtrasModule() {
		super("Extras", "Rucksack, Katzen-, Fuchs- oder Teufelsschwanz oder Schal in 3D", Category.COSMETICS, false);
		instance = this;
	}

	public static ExtrasModule get() {
		return instance;
	}

	public String currentId() {
		return CosmeticRegistry.idForName(CosmeticRegistry.EXTRAS, type.get());
	}
}
