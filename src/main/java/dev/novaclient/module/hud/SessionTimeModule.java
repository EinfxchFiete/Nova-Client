package dev.novaclient.module.hud;

import dev.novaclient.setting.ModeSetting;
import dev.novaclient.world.Playtime;

/** Play time in the current world/server: this session or in total (saved, see /playtime). */
public class SessionTimeModule extends TextHudModule {
	public final ModeSetting mode = add(new ModeSetting("Anzeige", "Diese Sitzung", "Diese Sitzung", "Gesamt in dieser Welt"));

	public SessionTimeModule() {
		super("Spielzeit", "Wie lange du in dieser Welt/auf diesem Server spielst (auch gesamt, /playtime)", false, Anchor.END, 2, Anchor.START, 98);
	}

	@Override
	protected String label() {
		return mode.is("Diese Sitzung") ? "Spielzeit" : "Gesamt";
	}

	@Override
	protected String value() {
		long ms = mc().level == null ? 0 : mode.is("Diese Sitzung") ? Playtime.currentSession() : Playtime.currentTotal();
		long s = ms / 1000L;
		long h = s / 3600;
		long m = (s % 3600) / 60;
		long sec = s % 60;
		return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
	}
}
