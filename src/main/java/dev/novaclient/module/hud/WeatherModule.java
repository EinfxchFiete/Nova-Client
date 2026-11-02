package dev.novaclient.module.hud;

public class WeatherModule extends TextHudModule {
	public WeatherModule() {
		super("Wetter", "Aktuelles Wetter: Klar, Regen oder Gewitter", false, Anchor.END, 2, Anchor.START, 250);
	}

	@Override
	protected String label() {
		return "Wetter";
	}

	@Override
	protected String value() {
		if (mc().level == null) {
			return "Klar";
		}
		if (mc().level.isThundering()) {
			return "Gewitter";
		}
		return mc().level.isRaining() ? "Regen" : "Klar";
	}
}
