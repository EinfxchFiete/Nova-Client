package dev.novaclient.module.hud;

import net.minecraft.client.player.LocalPlayer;

/** What you are doing right now: sprinting, sneaking, swimming, flying, gliding, ... */
public class MovementStatusModule extends TextHudModule {
	public MovementStatusModule() {
		super("Bewegungsstatus", "Zeigt, ob du sprintest, schleichst, schwimmst, fliegst oder gleitest", false, Anchor.START, 2, Anchor.START, 158);
	}

	@Override
	protected String label() {
		return "Status";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		if (p == null) {
			return "-";
		}
		if (p.isFallFlying()) {
			return "Gleitet";
		}
		if (p.getAbilities().flying) {
			return "Fliegt";
		}
		if (p.isSwimming()) {
			return "Schwimmt";
		}
		if (p.isPassenger()) {
			return "Reitet";
		}
		if (p.isShiftKeyDown()) {
			return "Schleicht";
		}
		if (p.isSprinting()) {
			return "Sprintet";
		}
		return p.getDeltaMovement().horizontalDistanceSqr() > 0.0005 ? "Läuft" : "Steht";
	}
}
