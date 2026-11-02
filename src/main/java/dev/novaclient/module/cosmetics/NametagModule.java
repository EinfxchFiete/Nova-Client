package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.BoolSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Shows your own nametag in third person and marks you with a Nova symbol. */
public class NametagModule extends Module {
	private static NametagModule instance;

	public final BoolSetting ownNametag = add(new BoolSetting("Eigener Nametag (F5)", true));
	public final BoolSetting symbol = add(new BoolSetting("Nova-Symbol vor dem Namen", true));

	public NametagModule() {
		super("Nametags", "Zeigt deinen eigenen Namen in der 3. Person mit Nova-Symbol", Category.COSMETICS, false);
		instance = this;
	}

	public static boolean showOwn() {
		return instance != null && instance.isEnabled() && instance.ownNametag.isOn();
	}

	public static Component decorate(Component name) {
		if (instance == null || !instance.isEnabled() || !instance.symbol.isOn() || name == null) {
			return name;
		}
		MutableComponent star = Component.literal("✦ ").withColor(ThemeModule.accent() & 0xFFFFFF);
		return star.append(name);
	}
}
