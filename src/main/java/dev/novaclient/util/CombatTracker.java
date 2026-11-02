package dev.novaclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Tracks the local player's hits for the reach display and combo counter. */
public final class CombatTracker {
	private static final long COMBO_TIMEOUT_MS = 3000L;

	private static double lastReach = -1;
	private static long lastHitTime;
	private static int combo;
	private static int lastHurtTime;

	private CombatTracker() {
	}

	/** Called when the local player attacks an entity. */
	public static void onAttack(LocalPlayer player, Entity target) {
		Vec3 eye = player.getEyePosition();
		AABB box = target.getBoundingBox();
		double x = Math.max(box.minX, Math.min(eye.x, box.maxX));
		double y = Math.max(box.minY, Math.min(eye.y, box.maxY));
		double z = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
		lastReach = eye.distanceTo(new Vec3(x, y, z));
		long now = System.currentTimeMillis();
		if (now - lastHitTime > COMBO_TIMEOUT_MS) {
			combo = 0;
		}
		combo++;
		lastHitTime = now;
	}

	/** Resets the combo when the player gets hurt or after a timeout. */
	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			combo = 0;
			lastHurtTime = 0;
			return;
		}
		if (p.hurtTime > lastHurtTime) {
			combo = 0;
		}
		lastHurtTime = p.hurtTime;
		if (combo > 0 && System.currentTimeMillis() - lastHitTime > COMBO_TIMEOUT_MS) {
			combo = 0;
		}
	}

	public static double lastReach() {
		return lastReach;
	}

	public static long lastHitTime() {
		return lastHitTime;
	}

	public static int combo() {
		return combo;
	}
}
