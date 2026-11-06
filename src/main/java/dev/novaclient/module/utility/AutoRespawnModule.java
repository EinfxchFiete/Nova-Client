package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/** Respawns automatically after dying (not in hardcore worlds). The death coordinates are still shown/saved. */
public class AutoRespawnModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Verzögerung (Sekunden)", 1.0, 0.0, 5.0, 0.5));
	private int deadTicks;
	private boolean sent;

	public AutoRespawnModule() {
		super("Auto-Respawn", "Nach dem Tod automatisch wieder einsteigen (nicht im Hardcore-Modus)", Category.UTILITY, false);
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (!isEnabled() || mc.player == null || mc.level == null || !(mc.gui.screen() instanceof DeathScreen)
			|| mc.level.getLevelData().isHardcore()) {
			deadTicks = 0;
			sent = false;
			return;
		}
		deadTicks++;
		if (!sent && deadTicks >= Math.max(1, Math.round(delay.get() * 20))) {
			sent = true;
			mc.player.respawn();
		}
	}
}
