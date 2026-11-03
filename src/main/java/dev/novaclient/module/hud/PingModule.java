package dev.novaclient.module.hud;

import dev.novaclient.util.PingTracker;

public class PingModule extends TextHudModule {
	public PingModule() {
		super("Ping", "Zeigt deine echte Latenz zum Server (gemessen, nicht der Tab-Wert)", true, Anchor.START, 2, Anchor.START, 18);
	}

	@Override
	protected String label() {
		return "Ping";
	}

	@Override
	protected String value() {
		if (mc().getConnection() == null || mc().player == null) {
			return "-";
		}
		int ms = PingTracker.current(mc());
		return ms < 0 ? "misst…" : ms + " ms";
	}
}
