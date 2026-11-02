package dev.novaclient.module.hud;

import java.util.Locale;
import net.minecraft.client.player.LocalPlayer;

public class SaturationModule extends TextHudModule {
	public SaturationModule() {
		super("Sättigung", "Versteckter Sättigungswert deines Hungers", false, Anchor.END, 2, Anchor.START, 202);
	}

	@Override
	protected String label() {
		return "Sättigung";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		return p == null ? "5.0" : String.format(Locale.ROOT, "%.1f", p.getFoodData().getSaturationLevel());
	}
}
