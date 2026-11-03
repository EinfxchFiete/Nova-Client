package dev.novaclient.module.hud;

import net.minecraft.client.player.LocalPlayer;

/** XP level, progress to the next level and the XP needed. */
public class ExperienceModule extends TextHudModule {
	public ExperienceModule() {
		super("Erfahrung", "Level, Fortschritt und fehlende XP bis zum nächsten Level", false, Anchor.START, 2, Anchor.START, 144);
	}

	@Override
	protected String label() {
		return "XP";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		if (p == null) {
			return "-";
		}
		int needed = p.getXpNeededForNextLevel();
		int missing = Math.max(0, Math.round(needed * (1 - p.experienceProgress)));
		return "Level " + p.experienceLevel + " · " + Math.round(p.experienceProgress * 100) + "% (noch " + missing + ")";
	}
}
