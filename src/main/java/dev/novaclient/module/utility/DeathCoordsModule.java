package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.ThemeModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Writes your death position into the chat (only you can see it). */
public class DeathCoordsModule extends Module {
	private boolean wasDead;

	public DeathCoordsModule() {
		super("Todes-Koordinaten", "Schreibt dir beim Tod die Position in den Chat", Category.UTILITY, true);
	}

	@Override
	public void tick() {
		LocalPlayer p = mc().player;
		boolean dead = p != null && p.isDeadOrDying();
		if (dead && !wasDead && isEnabled()) {
			String dim = p.level().dimension().identifier().getPath();
			String text = "Gestorben bei X " + p.blockPosition().getX() + ", Y " + p.blockPosition().getY() + ", Z "
				+ p.blockPosition().getZ() + " (" + dim + ")";
			mc().gui.hud.getChat().addClientSystemMessage(
				Component.literal("[Nova] ").withColor(ThemeModule.accent() & 0xFFFFFF).append(Component.literal(text).withColor(0xFFFFFF)));
		}
		wasDead = dead;
	}
}
