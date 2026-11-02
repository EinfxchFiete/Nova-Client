package dev.novaclient.module.hud;

import dev.novaclient.util.CombatTracker;

public class ComboModule extends TextHudModule {
	public ComboModule() {
		super("Combo", "Treffer in Folge ohne selbst getroffen zu werden", false, Anchor.CENTER, 50, Anchor.CENTER, 22);
	}

	@Override
	protected String label() {
		return "Combo";
	}

	@Override
	protected String value() {
		return Integer.toString(CombatTracker.combo());
	}
}
