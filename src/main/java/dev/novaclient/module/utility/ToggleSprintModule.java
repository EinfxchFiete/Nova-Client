package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import net.minecraft.client.Minecraft;

/** Keeps the sprint key pressed so you always sprint while walking forward. */
public class ToggleSprintModule extends Module {
	public ToggleSprintModule() {
		super("Toggle Sprint", "Immer sprinten, ohne die Sprint-Taste zu halten", Category.UTILITY, false);
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		if (isEnabled() && mc.player != null && mc.gui.screen() == null && !mc.options.keySprint.isDown()) {
			mc.options.keySprint.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		Minecraft mc = mc();
		if (mc.options.keySprint.isDown()) {
			// With vanilla "toggle sprint" enabled, setDown(true) flips the state off.
			mc.options.keySprint.setDown(!mc.options.toggleSprint().get() ? false : true);
		}
	}
}
