package dev.novaclient.module.hud;

import dev.novaclient.util.CombatTracker;
import java.util.Locale;

public class ReachModule extends TextHudModule {
	public ReachModule() {
		super("Reichweite", "Entfernung deines letzten Treffers", false, Anchor.CENTER, -50, Anchor.CENTER, 22);
	}

	@Override
	protected String label() {
		return "Reach";
	}

	@Override
	protected String value() {
		double r = CombatTracker.lastReach();
		if (r < 0 || System.currentTimeMillis() - CombatTracker.lastHitTime() > 5000L) {
			return "-";
		}
		return String.format(Locale.ROOT, "%.2f m", r);
	}
}
