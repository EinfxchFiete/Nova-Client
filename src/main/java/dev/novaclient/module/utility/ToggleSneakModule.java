package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;

/** Uses the vanilla "toggle crouch" option: press sneak once to start/stop sneaking. */
public class ToggleSneakModule extends Module {
	private Boolean previous;

	public ToggleSneakModule() {
		super("Toggle Sneak", "Schleichen per Tastendruck an/aus statt halten", Category.UTILITY, false);
	}

	@Override
	public void tick() {
		if (isEnabled() && !mc().options.toggleCrouch().get()) {
			if (previous == null) {
				previous = Boolean.FALSE;
			}
			mc().options.toggleCrouch().set(true);
		}
	}

	@Override
	protected void onDisable() {
		if (previous != null) {
			mc().options.toggleCrouch().set(previous);
			previous = null;
		}
	}
}
