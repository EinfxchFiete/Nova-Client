package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import dev.novaclient.util.CpsTracker;

public class CpsModule extends TextHudModule {
	public final BoolSetting showRight = add(new BoolSetting("Rechtsklick anzeigen", true));

	public CpsModule() {
		super("CPS", "Klicks pro Sekunde", true, Anchor.START, 2, Anchor.START, 34);
	}

	@Override
	protected String label() {
		return "CPS";
	}

	@Override
	protected String value() {
		return showRight.isOn() ? CpsTracker.left() + " | " + CpsTracker.right() : Integer.toString(CpsTracker.left());
	}
}
