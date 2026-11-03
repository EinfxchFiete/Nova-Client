package dev.novaclient.module.hud;

import net.minecraft.client.multiplayer.ClientLevel;

/** Minecraft time of day as a clock (06:00 = sunrise) and whether it is day or night. */
public class GameTimeModule extends TextHudModule {
	public GameTimeModule() {
		super("Ingame-Zeit", "Uhrzeit in Minecraft (z. B. 18:30, Nacht)", false, Anchor.END, 2, Anchor.START, 158);
	}

	@Override
	protected String label() {
		return "Ingame";
	}

	@Override
	protected String value() {
		ClientLevel level = mc().level;
		if (level == null) {
			return "-";
		}
		long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
		int hours = (int) ((t / 1000 + 6) % 24);
		int minutes = (int) (t % 1000 * 60 / 1000);
		boolean night = t >= 12542 && t < 23460;
		return String.format("%02d:%02d %s", hours, minutes, night ? "☾" : "☀");
	}
}
