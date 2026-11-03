package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.util.CombatTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

/** A visual effect where something you hit dies (only on your screen, never sent to the server). */
public class KillEffectModule extends Module {
	private static final int BOLT_ID = -4_242_043;

	public final ModeSetting effect = add(new ModeSetting("Effekt", "Totem", "Totem", "Blitz", "Herzen", "Flammen", "Seelen", "Explosion"));

	private Entity handled;
	private int boltTicks = -1;

	public KillEffectModule() {
		super("Kill-Effekt", "Blitz, Totem-Partikel und mehr, wenn etwas stirbt, das du getroffen hast", Category.COSMETICS, false);
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		ClientLevel level = mc.level;
		// the client-only lightning is not ticked by the game, so it would stay forever: remove it after one second
		if (boltTicks >= 0 && ++boltTicks > 20) {
			boltTicks = -1;
			if (level != null && level.getEntity(BOLT_ID) != null) {
				level.removeEntity(BOLT_ID, Entity.RemovalReason.DISCARDED);
			}
		}
		Entity target = CombatTracker.lastHitEntity();
		if (!isEnabled() || level == null || mc.player == null || target == null || target == handled || target == mc.player) {
			return;
		}
		if (System.currentTimeMillis() - CombatTracker.lastHitMillis() > 5000 || target.level() != level) {
			return;
		}
		if (target instanceof LivingEntity living && living.isDeadOrDying()) {
			handled = target;
			play(level, target);
		}
	}

	/** Plays the effect at the entity (also used by tests). */
	public void play(ClientLevel level, Entity at) {
		double x = at.getX();
		double y = at.getY();
		double z = at.getZ();
		if (effect.is("Blitz")) {
			Entity old = level.getEntity(BOLT_ID);
			if (old != null) {
				level.removeEntity(BOLT_ID, Entity.RemovalReason.DISCARDED);
			}
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
			if (bolt != null) {
				bolt.setId(BOLT_ID);
				bolt.setVisualOnly(true);
				bolt.snapTo(x, y, z, 0, 0);
				level.addEntity(bolt);
				boltTicks = 0;
			}
			return;
		}
		SimpleParticleType type = switch (effect.get()) {
			case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
			case "Herzen" -> ParticleTypes.HEART;
			case "Flammen" -> ParticleTypes.FLAME;
			case "Seelen" -> ParticleTypes.SOUL;
			default -> ParticleTypes.EXPLOSION;
		};
		int count = type == ParticleTypes.EXPLOSION ? 6 : 40;
		for (int i = 0; i < count; i++) {
			double vx = (Math.random() - 0.5) * 0.6;
			double vy = Math.random() * 0.5 + 0.1;
			double vz = (Math.random() - 0.5) * 0.6;
			level.addParticle(type, x + (Math.random() - 0.5), y + Math.random() * at.getBbHeight(), z + (Math.random() - 0.5), vx, vy, vz);
		}
	}
}
