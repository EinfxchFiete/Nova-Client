package dev.novaclient.module.hud;

import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;

/** Current moon phase (affects slime spawns and mob equipment). */
public class MoonPhaseModule extends TextHudModule {
	public MoonPhaseModule() {
		super("Mondphase", "Zeigt die aktuelle Mondphase", false, Anchor.END, 2, Anchor.START, 172);
	}

	@Override
	protected String label() {
		return "Mond";
	}

	@Override
	protected String value() {
		if (mc().level == null || mc().player == null) {
			return "-";
		}
		MoonPhase phase = mc().level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, mc().player.position());
		return switch (phase) {
			case FULL_MOON -> "Vollmond";
			case WANING_GIBBOUS -> "Abnehmend (dreiviertel)";
			case THIRD_QUARTER -> "Abnehmender Halbmond";
			case WANING_CRESCENT -> "Abnehmende Sichel";
			case NEW_MOON -> "Neumond";
			case WAXING_CRESCENT -> "Zunehmende Sichel";
			case FIRST_QUARTER -> "Zunehmender Halbmond";
			case WAXING_GIBBOUS -> "Zunehmend (dreiviertel)";
		};
	}
}
