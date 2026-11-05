package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;

/**
 * More FPS: entities and block entities (chests, signs, banners, ...) far away are not drawn and the number of
 * particles is limited. Only affects what you see, nothing in the world changes.
 */
public class FpsBoostModule extends Module {
	private static FpsBoostModule instance;

	public final NumberSetting entityDistance = add(new NumberSetting("Entity-Sichtweite", 64, 16, 160, 8));
	public final BoolSetting alwaysPlayers = add(new BoolSetting("Spieler immer anzeigen", true));
	public final NumberSetting blockEntityDistance = add(new NumberSetting("Block-Entity-Sichtweite", 48, 16, 64, 4));
	public final NumberSetting maxParticles = add(new NumberSetting("Max. Partikel", 4000, 500, 16000, 500));

	public FpsBoostModule() {
		super("FPS-Boost", "Mehr FPS: weit entfernte Entities und Block-Entities (Kisten, Schilder) ausblenden, Partikel begrenzen",
			Category.VISUAL, true);
		instance = this;
	}

	public static FpsBoostModule get() {
		return instance;
	}

	private static boolean on() {
		return instance != null && instance.isEnabled();
	}

	/** False if the entity is too far away to be drawn. */
	public static boolean shouldDraw(Entity e, double camX, double camY, double camZ) {
		if (!on()) {
			return true;
		}
		FpsBoostModule m = instance;
		if (e instanceof Player && m.alwaysPlayers.isOn() || e instanceof EnderDragon || e instanceof WitherBoss) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		if (e == mc.getCameraEntity() || e.getId() < 0 || mc.player != null && (e.hasPassenger(mc.player) || mc.player.getVehicle() == e)) {
			return true;
		}
		double max = m.entityDistance.get();
		return e.distanceToSqr(camX, camY, camZ) <= max * max;
	}

	/** Squared distance up to which block entities are drawn, or -1 for the vanilla distance. */
	public static double blockEntityDistanceSq() {
		if (!on()) {
			return -1;
		}
		double d = instance.blockEntityDistance.get();
		return d * d;
	}

	/** Maximum particles per particle group, or -1 for vanilla. */
	public static int particleLimit() {
		return on() ? instance.maxParticles.getInt() : -1;
	}
}
