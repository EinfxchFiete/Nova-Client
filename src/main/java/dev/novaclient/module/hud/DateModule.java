package dev.novaclient.module.hud;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateModule extends TextHudModule {
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	public DateModule() {
		super("Datum", "Heutiges Datum", false, Anchor.END, 2, Anchor.START, 218);
	}

	@Override
	protected String label() {
		return "Datum";
	}

	@Override
	protected String value() {
		return LocalDate.now().format(FORMAT);
	}
}
