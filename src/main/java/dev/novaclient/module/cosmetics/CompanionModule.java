package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A little buddy that floats next to you. It only exists on your client (negative entity id,
 * never sent to the server), so nobody else sees it and it cannot interact with the world.
 */
public class CompanionModule extends Module {
	private static final int ENTITY_ID = -4_242_042;

	public final ModeSetting type = add(new ModeSetting("Begleiter", "Allay", "Allay", "Biene", "Papagei", "Vex"));
	public final ModeSetting side = add(new ModeSetting("Seite", "Rechts", "Rechts", "Links"));

	private Entity buddy;
	private String spawnedType;
	private float bob;

	public CompanionModule() {
		super("Begleiter", "Ein kleiner Begleiter schwebt neben dir (Allay, Biene, ...)", Category.COSMETICS, false);
	}

	private EntityType<?> entityType() {
		return switch (type.get()) {
			case "Biene" -> EntityTypes.BEE;
			case "Papagei" -> EntityTypes.PARROT;
			case "Vex" -> EntityTypes.VEX;
			default -> EntityTypes.ALLAY;
		};
	}

	public Entity getBuddy() {
		return buddy;
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		ClientLevel level = mc.level;
		LocalPlayer p = mc.player;
		if (!isEnabled() || level == null || p == null) {
			despawn();
			return;
		}
		if (buddy == null || buddy.level() != level || buddy.isRemoved() || !type.get().equals(spawnedType)) {
			despawn();
			spawn(level, p);
			if (buddy == null) {
				return;
			}
		}
		// target: behind the shoulder, gently bobbing
		bob += 0.12F;
		float yaw = p.getYRot() * Mth.DEG_TO_RAD;
		double sideSign = side.is("Links") ? -1 : 1;
		double ox = -Math.sin(yaw) * -0.6 + Math.cos(yaw) * 0.9 * sideSign;
		double oz = Math.cos(yaw) * -0.6 + Math.sin(yaw) * 0.9 * sideSign;
		Vec3 target = new Vec3(p.getX() + ox, p.getY() + 1.6 + Math.sin(bob) * 0.12, p.getZ() + oz);
		Vec3 cur = buddy.position();
		Vec3 next = cur.distanceToSqr(target) > 64 ? target : cur.lerp(target, 0.35);
		buddy.setPos(next.x, next.y, next.z);
		buddy.setDeltaMovement(Vec3.ZERO);
		buddy.setYRot(p.getYRot());
		if (buddy instanceof LivingEntity living) {
			living.yBodyRot = p.getYRot();
			living.setYHeadRot(p.getYHeadRot());
		}
	}

	private void spawn(ClientLevel level, LocalPlayer p) {
		Entity e = entityType().create(level, EntitySpawnReason.COMMAND);
		if (e == null) {
			return;
		}
		e.setId(ENTITY_ID);
		e.setNoGravity(true);
		e.setSilent(true);
		e.noPhysics = true;
		e.snapTo(p.getX(), p.getY() + 1.6, p.getZ(), p.getYRot(), 0);
		level.addEntity(e);
		buddy = e;
		spawnedType = type.get();
	}

	private void despawn() {
		if (buddy != null) {
			ClientLevel level = mc().level;
			if (level != null && level.getEntity(ENTITY_ID) == buddy) {
				level.removeEntity(ENTITY_ID, Entity.RemovalReason.DISCARDED);
			}
			buddy = null;
			spawnedType = null;
		}
	}

	@Override
	protected void onDisable() {
		despawn();
	}
}
