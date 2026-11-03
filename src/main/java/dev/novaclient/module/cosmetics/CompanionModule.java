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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * A little buddy that floats next to you. It only exists on your client (negative entity id,
 * never sent to the server), so nobody else sees it and it cannot interact with the world.
 */
public class CompanionModule extends Module {
	private static final int ENTITY_ID = -4_242_042;

	public final ModeSetting type = add(new ModeSetting("Begleiter", "Allay", "Allay", "Biene", "Papagei", "Vex", "Fledermaus",
		"Axolotl", "Frosch", "Fuchs", "Katze", "Wolf", "Panda", "Mini-Ghast", "Lohe", "Breeze", "Kupfergolem", "Schneegolem",
		"Leuchttintenfisch", "Schleim", "Hase"));
	public final ModeSetting side = add(new ModeSetting("Seite", "Rechts", "Rechts", "Links"));

	private Entity buddy;
	private String spawnedType;
	private float bob;

	public CompanionModule() {
		super("Begleiter", "Ein kleiner Begleiter schwebt neben dir (19 Tiere: Allay, Fuchs, Mini-Ghast, ...)", Category.COSMETICS, false);
	}

	private EntityType<?> entityType() {
		return switch (type.get()) {
			case "Biene" -> EntityTypes.BEE;
			case "Papagei" -> EntityTypes.PARROT;
			case "Vex" -> EntityTypes.VEX;
			case "Fledermaus" -> EntityTypes.BAT;
			case "Axolotl" -> EntityTypes.AXOLOTL;
			case "Frosch" -> EntityTypes.FROG;
			case "Fuchs" -> EntityTypes.FOX;
			case "Katze" -> EntityTypes.CAT;
			case "Wolf" -> EntityTypes.WOLF;
			case "Panda" -> EntityTypes.PANDA;
			case "Mini-Ghast" -> EntityTypes.GHAST;
			case "Lohe" -> EntityTypes.BLAZE;
			case "Breeze" -> EntityTypes.BREEZE;
			case "Kupfergolem" -> EntityTypes.COPPER_GOLEM;
			case "Schneegolem" -> EntityTypes.SNOW_GOLEM;
			case "Leuchttintenfisch" -> EntityTypes.GLOW_SQUID;
			case "Schleim" -> EntityTypes.SLIME;
			case "Hase" -> EntityTypes.RABBIT;
			default -> EntityTypes.ALLAY;
		};
	}

	/** Size relative to the normal mob, so every buddy is about as big as an allay. */
	private double scale() {
		return switch (type.get()) {
			case "Axolotl", "Frosch" -> 0.7;
			case "Fuchs", "Katze", "Wolf" -> 0.55;
			case "Panda" -> 0.3;
			case "Mini-Ghast" -> 0.12;
			case "Lohe", "Breeze" -> 0.4;
			case "Kupfergolem" -> 0.6;
			case "Schneegolem" -> 0.32;
			case "Leuchttintenfisch" -> 0.45;
			case "Schleim" -> 0.5;
			case "Hase" -> 0.8;
			default -> 1.0;
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
		if (e instanceof LivingEntity living) {
			AttributeInstance size = living.getAttribute(Attributes.SCALE);
			if (size != null) {
				size.setBaseValue(scale());
			}
		}
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
