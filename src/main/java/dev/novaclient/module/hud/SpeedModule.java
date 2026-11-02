package dev.novaclient.module.hud;

import java.util.Locale;
import net.minecraft.client.player.LocalPlayer;

public class SpeedModule extends TextHudModule {
	public SpeedModule() {
		super("Geschwindigkeit", "Horizontale Geschwindigkeit in Blöcken/s", false, Anchor.END, 2, Anchor.START, 34);
	}

	@Override
	protected String label() {
		return "Speed";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		if (p == null) {
			return "0.00 b/s";
		}
		double dx = p.getX() - p.xo;
		double dz = p.getZ() - p.zo;
		return String.format(Locale.ROOT, "%.2f b/s", Math.sqrt(dx * dx + dz * dz) * 20.0);
	}
}
