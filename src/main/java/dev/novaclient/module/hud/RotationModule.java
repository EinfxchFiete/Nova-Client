package dev.novaclient.module.hud;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** Exact viewing direction (yaw / pitch), useful for building and bridging. */
public class RotationModule extends TextHudModule {
	public RotationModule() {
		super("Blickrichtung", "Zeigt Yaw und Pitch deiner Blickrichtung", false, Anchor.START, 2, Anchor.START, 130);
	}

	@Override
	protected String label() {
		return "Blick";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		if (p == null) {
			return "-";
		}
		return String.format(java.util.Locale.ROOT, "%.1f / %.1f", Mth.wrapDegrees(p.getYRot()), p.getXRot());
	}
}
