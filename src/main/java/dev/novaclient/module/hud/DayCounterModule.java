package dev.novaclient.module.hud;

public class DayCounterModule extends TextHudModule {
	public DayCounterModule() {
		super("Tag-Zähler", "Aktueller Ingame-Tag", false, Anchor.END, 2, Anchor.START, 66);
	}

	@Override
	protected String label() {
		return "Tag";
	}

	@Override
	protected String value() {
		if (mc().level == null) {
			return "0";
		}
		return Long.toString(mc().level.getOverworldClockTime() / 24000L);
	}
}
