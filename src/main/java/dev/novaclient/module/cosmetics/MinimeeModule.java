package dev.novaclient.module.cosmetics;

import dev.novaclient.cosmetics.CosmeticRegistry;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.TextSetting;

/** A mini version of any player (by Minecraft name): as necklace, holding on to your pocket or sitting on your head. */
public class MinimeeModule extends Module {
	private static MinimeeModule instance;

	public final TextSetting name = add(new TextSetting("Spielername", "", 16));
	public final ModeSetting mode = add(new ModeSetting("Position", "Kette", CosmeticRegistry.MINIMEE_MODES.values().toArray(new String[0])));
	public final ModeSetting anim = add(new ModeSetting("Animation", "Keine", CosmeticRegistry.MINIMEE_ANIMS.values().toArray(new String[0])));
	public final ModeSetting size = add(new ModeSetting("Größe", "Normal", CosmeticRegistry.MINIMEE_SIZES.values().toArray(new String[0])));

	public MinimeeModule() {
		super("Minimee", "Mini-Version eines Spielers: Kette, Hosentasche, Kopf, Schulter, Huckepack, Hand oder schwebend", Category.COSMETICS, false);
		instance = this;
	}

	public static MinimeeModule get() {
		return instance;
	}

	/** The entered name if valid, otherwise "" (an empty name means: your own skin). */
	public String playerName() {
		String n = CosmeticRegistry.validName(name.get().trim());
		if (n.isEmpty() && name.get().isBlank() && mc().player != null) {
			n = mc().player.getGameProfile().name();
		}
		return n;
	}

	public String modeId() {
		return CosmeticRegistry.idForName(CosmeticRegistry.MINIMEE_MODES, mode.get());
	}

	public String animId() {
		return CosmeticRegistry.validMinimeeAnim(CosmeticRegistry.idForName(CosmeticRegistry.MINIMEE_ANIMS, anim.get()));
	}

	public String sizeId() {
		return CosmeticRegistry.validMinimeeSize(CosmeticRegistry.idForName(CosmeticRegistry.MINIMEE_SIZES, size.get()));
	}
}
