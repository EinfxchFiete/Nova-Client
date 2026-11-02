package dev.novaclient.module.hud;

public class FpsModule extends TextHudModule {
	public FpsModule() {
		super("FPS", "Zeigt deine Bilder pro Sekunde", true, Anchor.START, 2, Anchor.START, 2);
	}

	@Override
	protected String label() {
		return "FPS";
	}

	@Override
	protected String value() {
		return Integer.toString(mc().getFps());
	}
}
