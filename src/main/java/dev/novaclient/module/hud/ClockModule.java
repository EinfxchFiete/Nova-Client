package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockModule extends TextHudModule {
	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

	public final BoolSetting seconds = add(new BoolSetting("Sekunden", false));

	public ClockModule() {
		super("Uhrzeit", "Echte Uhrzeit", false, Anchor.END, 2, Anchor.START, 2);
	}

	@Override
	protected String label() {
		return "Zeit";
	}

	@Override
	protected String value() {
		return LocalTime.now().format(seconds.isOn() ? HMS : HM);
	}
}
