package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Shows the health of players next to their name tag. */
public class HealthTagsModule extends Module {
	private static HealthTagsModule instance;

	public final ModeSetting style = add(new ModeSetting("Anzeige", "Herzen", "Herzen", "Lebenspunkte", "Prozent"));

	public HealthTagsModule() {
		super("Leben über Spielern", "Zeigt das Leben anderer Spieler neben ihrem Namen", Category.VISUAL, false);
		instance = this;
	}

	/** The name with health appended, or the name unchanged. */
	public static Component decorate(Player p, Component name) {
		HealthTagsModule m = instance;
		if (m == null || !m.isEnabled() || name == null) {
			return name;
		}
		float hp = p.getHealth() + p.getAbsorptionAmount();
		float max = p.getMaxHealth();
		float f = max <= 0 ? 0 : Math.min(1, p.getHealth() / max);
		int color = f > 0.6F ? 0x55FF55 : f > 0.3F ? 0xFFDD44 : 0xFF5555;
		String text = switch (m.style.get()) {
			case "Lebenspunkte" -> String.format(Locale.ROOT, "%.0f", hp);
			case "Prozent" -> Math.round(f * 100) + "%";
			default -> String.format(Locale.ROOT, hp % 2 < 0.05 ? "%.0f" : "%.1f", hp / 2) + "❤";
		};
		return name.copy().append(Component.literal(" " + text).withColor(color));
	}
}
