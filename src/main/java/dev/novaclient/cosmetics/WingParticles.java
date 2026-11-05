package dev.novaclient.cosmetics;

import dev.novaclient.module.cosmetics.WingsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

/**
 * Particles of cosmetics (yours and other Nova players'): small flames from phoenix wings, sparkles from fairy and ice
 * wings, jetpack flames while you are in the air and music notes from the guitar while a song is playing.
 */
public final class WingParticles {
	private static int ticks;
	private static int jetpackFlames;

	private WingParticles() {
	}

	/** Number of jetpack flames spawned so far (tests). */
	public static int jetpackFlames() {
		return jetpackFlames;
	}

	public static void tick(Minecraft mc) {
		WingsModule wm = WingsModule.get();
		if (mc.level == null || mc.player == null || mc.isPaused()) {
			return;
		}
		boolean wingParticles = wm != null && wm.particles.isOn();
		ticks++;
		for (AbstractClientPlayer p : mc.level.players()) {
			if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(mc.player) > 48 * 48
				|| p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
				continue;
			}
			Loadout l = Cosmetics.loadoutFor(p.getId());
			if (l == null) {
				continue;
			}
			extras(mc, p, l);
			if (l.wings().isEmpty() || !wingParticles) {
				continue;
			}
			boolean phoenix = l.wings().equals("phoenix");
			boolean fairy = l.wings().equals("fairy") || l.wings().equals("ice");
			if (!phoenix && !fairy || (ticks + p.getId()) % (phoenix ? 4 : 7) != 0) {
				continue;
			}
			// somewhere along the wings behind the back: body yaw, half a block to each side, shoulder to hip height
			float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
			double side = (p.getRandom().nextBoolean() ? 1 : -1) * (0.25 + p.getRandom().nextDouble() * 0.55);
			double back = 0.3 + p.getRandom().nextDouble() * 0.15;
			double x = p.getX() - Math.cos(yaw) * side - Math.sin(yaw) * -back;
			double z = p.getZ() - Math.sin(yaw) * side + Math.cos(yaw) * -back;
			double y = p.getY() + (p.isCrouching() ? 0.9 : 1.1) + p.getRandom().nextDouble() * 0.45;
			if (phoenix) {
				// small flames only: the big ones looked like the player was burning
				mc.level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.015, 0);
			} else if (l.wings().equals("ice")) {
				mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.02, 0);
			} else {
				mc.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, -0.01, 0);
			}
		}
	}

	/** A point behind the back: {@code side} blocks to the left (+) / right (-), {@code back} blocks behind, at {@code height}. */
	private static double[] behind(AbstractClientPlayer p, double side, double back, double height) {
		float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
		double x = p.getX() - Math.cos(yaw) * side + Math.sin(yaw) * back;
		double z = p.getZ() - Math.sin(yaw) * side - Math.cos(yaw) * back;
		return new double[] {x, p.getY() + height, z};
	}

	private static void extras(Minecraft mc, AbstractClientPlayer p, Loadout l) {
		switch (l.extra()) {
			case "jetpack" -> {
				// flames from both nozzles while you jump, fall or fly
				if (p.onGround() || p.isInWater() || p.isPassenger() || (ticks + p.getId()) % 2 != 0) {
					return;
				}
				double h = p.isCrouching() ? 0.75 : 0.95;
				for (int side = -1; side <= 1; side += 2) {
					double[] pos = behind(p, side * 0.15, 0.28, h);
					mc.level.addParticle(ParticleTypes.SMALL_FLAME, pos[0], pos[1], pos[2], 0, -0.08, 0);
					jetpackFlames++;
					if (p.getRandom().nextInt(3) == 0) {
						mc.level.addParticle(ParticleTypes.SMOKE, pos[0], pos[1] - 0.1, pos[2], 0, -0.05, 0);
					}
				}
			}
			case "guitar" -> {
				// a music note now and then while a song is playing
				if ((ticks + p.getId()) % 30 != 0) {
					return;
				}
				boolean music = p == mc.player
					? dev.novaclient.music.NowPlaying.current() != null && dev.novaclient.music.NowPlaying.current().playing()
					: dev.novaclient.network.NovaNetwork.song(p.getUUID()) != null;
				if (music) {
					double[] pos = behind(p, -0.25, 0.3, 1.9);
					mc.level.addParticle(ParticleTypes.NOTE, pos[0], pos[1], pos[2], p.getRandom().nextInt(25) / 24.0, 0, 0);
				}
			}
			default -> {
			}
		}
	}
}
