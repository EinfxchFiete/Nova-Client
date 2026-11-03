package dev.novaclient.cosmetics;

import dev.novaclient.NovaClient;
import dev.novaclient.module.cosmetics.CapeModule;
import dev.novaclient.module.cosmetics.HeadCosmeticModule;
import dev.novaclient.module.cosmetics.MinimeeModule;
import dev.novaclient.module.cosmetics.WingsModule;
import dev.novaclient.network.NovaNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/** Resolves what a rendered player wears: the local player from the modules, others from the Nova network. */
public final class Cosmetics {
	private Cosmetics() {
	}

	/** Loadout of the local player, built from the cosmetics modules. */
	public static Loadout local() {
		CapeModule cape = CapeModule.get();
		HeadCosmeticModule head = HeadCosmeticModule.get();
		WingsModule wings = WingsModule.get();
		MinimeeModule mini = MinimeeModule.get();
		boolean miniOn = mini != null && mini.isEnabled() && !mini.playerName().isEmpty();
		return new Loadout(
			cape != null && cape.isEnabled() ? cape.currentId() : "",
			head != null && head.isEnabled() ? head.currentId() : "",
			head != null && head.isEnabled() ? head.color.get() : "",
			wings != null && wings.isEnabled() ? wings.currentId() : "",
			wings != null && wings.isEnabled() ? wings.color.get() : "",
			miniOn ? mini.playerName() : "",
			miniOn ? mini.modeId() : "");
	}

	/** Loadout for the player with this entity id, or null if nothing should be drawn. */
	public static Loadout loadoutFor(int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && entityId == mc.player.getId()) {
			return local();
		}
		if (mc.level == null || NovaClient.modules() == null) {
			return null;
		}
		Entity e = mc.level.getEntity(entityId);
		if (e instanceof AbstractClientPlayer p) {
			return NovaNetwork.loadout(p.getUUID());
		}
		return null;
	}
}
