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
	public final dev.novaclient.setting.ModeSetting color = add(new dev.novaclient.setting.ModeSetting("Namensfarbe", "Normal",
		"Normal", "Theme", "Regenbogen", "Gold", "Rot", "Grün", "Blau", "Pink"));

	public NametagModule() {
		super("Nametags", "Eigener Name in der 3. Person, Nova-Symbol und Namensfarbe", Category.COSMETICS, false);
		instance = this;
	}

	public static boolean showOwn() {
		return instance != null && instance.isEnabled() && instance.ownNametag.isOn();
	}

	private int nameColor() {
		return switch (color.get()) {
			case "Theme" -> ThemeModule.accent() & 0xFFFFFF;
			case "Regenbogen" -> java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000F, 0.7F, 1F) & 0xFFFFFF;
			case "Gold" -> 0xFFC830;
			case "Rot" -> 0xFF5050;
			case "Grün" -> 0x55FF77;
			case "Blau" -> 0x5599FF;
			case "Pink" -> 0xFF77DD;
			default -> -1;
		};
	}

	public static Component decorate(Component name) {
		NametagModule n = instance;
		if (n == null || !n.isEnabled() || name == null) {
			return name;
		}
		int c = n.nameColor();
		Component shown = c < 0 ? name : Component.literal(name.getString()).withColor(c);
		if (!n.symbol.isOn()) {
			return shown;
		}
		MutableComponent star = Component.literal("✦ ").withColor(ThemeModule.accent() & 0xFFFFFF);
		return star.append(shown);
	}
}
