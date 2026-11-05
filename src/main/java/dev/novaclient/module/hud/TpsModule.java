package dev.novaclient.module.hud;

import dev.novaclient.util.TpsTracker;

/** Shows how fast the server runs (20 TPS = no lag). */
public class TpsModule extends TextHudModule {
	public TpsModule() {
		super("TPS", "Zeigt, wie schnell der Server läuft (20 TPS = kein Lag)", false, Anchor.START, 2, Anchor.START, 98);
	}

	@Override
	protected String label() {
		return "TPS";
	}

	@Override
	protected String value() {
		float tps = TpsTracker.tps();
		if (tps < 0) {
			return "misst…";
		}
		String lag = tps >= 18.5F ? "" : tps >= 14 ? " (etwas Lag)" : " (Lag!)";
		return String.format(java.util.Locale.ROOT, "%.1f", tps) + lag;
	}
}
