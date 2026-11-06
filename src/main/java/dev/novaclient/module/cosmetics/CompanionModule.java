package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A little buddy next to you. Flying buddies hover at your shoulder, the others walk (or hop) beside you on the ground
 * with their real walking animation. It only exists on your client (negative entity id, never sent to the server),
 * so nobody else sees it and it cannot interact with the world.
 *
 * Movement goes through the entity's own interpolation (like mobs the server moves), so Minecraft computes the walk
 * animation and body rotation itself – setting the position directly made walkers glide without moving their legs.
 */
public class CompanionModule extends Module {
	private static final int ENTITY_ID = -4_242_042;

	private enum Motion { FLY, WALK, HOP }

	/** One buddy type: entity, size and how it moves. {@code baby}: spawn the baby version instead of scaling. */
	private record Kind(EntityType<?> type, double scale, Motion motion, boolean baby, @Nullable String model) {
		Kind(EntityType<?> type, double scale, Motion motion, boolean baby) {
			this(type, scale, motion, baby, null);
		}
	}

	private static final Map<String, Kind> KINDS = new LinkedHashMap<>();

	static {
		KINDS.put("Allay", new Kind(EntityTypes.ALLAY, 1.0, Motion.FLY, false));
		KINDS.put("Biene", new Kind(EntityTypes.BEE, 1.0, Motion.FLY, false));
		KINDS.put("Papagei", new Kind(EntityTypes.PARROT, 1.0, Motion.FLY, false));
		KINDS.put("Vex", new Kind(EntityTypes.VEX, 1.0, Motion.FLY, false));
		KINDS.put("Fledermaus", new Kind(EntityTypes.BAT, 1.0, Motion.FLY, false));
		KINDS.put("Mini-Ghast", new Kind(EntityTypes.GHAST, 0.12, Motion.FLY, false));
		KINDS.put("Lohe", new Kind(EntityTypes.BLAZE, 0.4, Motion.FLY, false));
		KINDS.put("Breeze", new Kind(EntityTypes.BREEZE, 0.45, Motion.FLY, false));
		KINDS.put("Phantom", new Kind(EntityTypes.PHANTOM, 0.45, Motion.FLY, false));
		KINDS.put("Happy Ghast", new Kind(EntityTypes.HAPPY_GHAST, 0.12, Motion.FLY, false));
		KINDS.put("Fuchs", new Kind(EntityTypes.FOX, 1.0, Motion.WALK, true));
		KINDS.put("Katze", new Kind(EntityTypes.CAT, 1.0, Motion.WALK, true));
		KINDS.put("Wolf", new Kind(EntityTypes.WOLF, 1.0, Motion.WALK, true));
		KINDS.put("Panda", new Kind(EntityTypes.PANDA, 0.7, Motion.WALK, true));
		KINDS.put("Schwein", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true));
		KINDS.put("Huhn", new Kind(EntityTypes.CHICKEN, 1.0, Motion.WALK, true));
		KINDS.put("Gürteltier", new Kind(EntityTypes.ARMADILLO, 1.0, Motion.WALK, true));
		KINDS.put("Schnüffler", new Kind(EntityTypes.SNIFFER, 0.55, Motion.WALK, true));
		KINDS.put("Axolotl", new Kind(EntityTypes.AXOLOTL, 0.8, Motion.WALK, false));
		KINDS.put("Frosch", new Kind(EntityTypes.FROG, 0.8, Motion.WALK, false));
		KINDS.put("Kupfergolem", new Kind(EntityTypes.COPPER_GOLEM, 0.6, Motion.WALK, false));
		KINDS.put("Schneegolem", new Kind(EntityTypes.SNOW_GOLEM, 0.32, Motion.WALK, false));
		KINDS.put("Hase", new Kind(EntityTypes.RABBIT, 1.0, Motion.HOP, false));
		KINDS.put("Schleim", new Kind(EntityTypes.SLIME, 1.0, Motion.HOP, false));
		// pets with their own model: an invisible allay does the flying, PetRenderer draws the model
		KINDS.put("Blobfisch", new Kind(EntityTypes.ALLAY, 1.0, Motion.FLY, false, "blobfish"));
		// CUBEES by Cubes Studio (MCModels), used with the permission of Cubes Studio (see README, Credits):
		// an invisible baby pig does the walking, the baby ghast floats on an invisible allay
		KINDS.put("Cubee Feuer", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_fire"));
		KINDS.put("Cubee Wasser", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_water"));
		KINDS.put("Cubee Gras", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_grass"));
		KINDS.put("Cubee Stein", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_stone"));
		KINDS.put("Cubee TNT", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_tnt"));
		KINDS.put("Cubee Skelett", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_skeleton"));
		KINDS.put("Cubee Engel", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_good"));
		KINDS.put("Cubee Teufel", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_evil"));
		KINDS.put("Cubee Wüste", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_desert"));
		KINDS.put("Cubee Dschungel", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_jungle"));
		KINDS.put("Cubee Blüte", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_lush"));
		KINDS.put("Cubee Pilz", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_mushroom"));
		KINDS.put("Cubee Sumpf", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_swamp"));
		KINDS.put("Cubee Ebene", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_plains"));
		KINDS.put("Cubee Savanne", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_savanna"));
		KINDS.put("Cubee Schnee", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_snow"));
		KINDS.put("Cubee Baby-Ghast", new Kind(EntityTypes.ALLAY, 1.0, Motion.FLY, false, "cubee_baby_ghast"));
		KINDS.put("Cubee Bomber-Creeper", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_bombardier_creeper"));
		KINDS.put("Cubee Kanone", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_cannon"));
		KINDS.put("Cubee Hoodie-Fuchs", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_hoody_fox"));
		KINDS.put("Cubee Lama", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_llama"));
		KINDS.put("Cubee Steinschleuder", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_slingshot"));
		KINDS.put("Cubee Swag-Plünderer", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_swag_pillager"));
		KINDS.put("Cubee Glitzerhexe", new Kind(EntityTypes.PIG, 1.0, Motion.WALK, true, "cubee_twinkle_witch"));
	}

	public final ModeSetting type = add(new ModeSetting("Begleiter", "Allay", KINDS.keySet().toArray(new String[0])));
	public final ModeSetting side = add(new ModeSetting("Seite", "Rechts", "Rechts", "Links"));

	private @Nullable Entity buddy;
	private String spawnedType;
	private float bob;
	private float hopPhase;
	private int hopTicks;

	public CompanionModule() {
		super("Begleiter", "Ein Begleiter neben dir: " + KINDS.size() + " Tiere, Flugtiere schweben, die anderen laufen mit", Category.COSMETICS, false);
		instance = this;
	}

	private Kind kind() {
		return KINDS.getOrDefault(type.get(), KINDS.get("Allay"));
	}

	public @Nullable Entity getBuddy() {
		return buddy;
	}

	private static CompanionModule instance;

	public static CompanionModule get() {
		return instance;
	}

	/** Name of the own pet model of the current buddy (see PetModels), or null for a normal mob. */
	public @Nullable String customModel() {
		return buddy == null ? null : kind().model();
	}

	private boolean frozenForTest;

	/** Test hook: the buddy stops following (for close-up screenshots). */
	public void freezeForTest(boolean frozen) {
		frozenForTest = frozen;
	}

	/** True if the current buddy flies (pets with their own model use the fly or walk animation). */
	public boolean flies() {
		return kind().motion() == Motion.FLY;
	}

	/** True if the chosen buddy walks/hops on the ground (tests). */
	public boolean walks() {
		return kind().motion() != Motion.FLY;
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
		if (frozenForTest) {
			return;
		}
		Kind k = kind();
		float yawRad = p.getYRot() * Mth.DEG_TO_RAD;
		double sideSign = side.is("Links") ? -1 : 1;
		// right of the player = (-cos yaw, -sin yaw), behind = (sin yaw, -cos yaw)
		double rightX = -Math.cos(yawRad) * sideSign;
		double rightZ = -Math.sin(yawRad) * sideSign;
		double backX = Math.sin(yawRad);
		double backZ = -Math.cos(yawRad);
		if (k.motion() == Motion.FLY) {
			fly(p, rightX, rightZ, backX, backZ);
		} else {
			walk(level, p, k, rightX, rightZ, backX, backZ);
		}
	}

	/** Hovers behind the shoulder, gently bobbing. */
	private void fly(LocalPlayer p, double rightX, double rightZ, double backX, double backZ) {
		bob += 0.12F;
		Vec3 target = new Vec3(p.getX() + rightX * 0.9 + backX * 0.6, p.getY() + 1.6 + Math.sin(bob) * 0.12, p.getZ() + rightZ * 0.9 + backZ * 0.6);
		Entity b = buddy;
		if (b.position().distanceToSqr(target) > 64) {
			teleport(b, target, p.getYRot());
			return;
		}
		b.setOnGround(false);
		b.setDeltaMovement(Vec3.ZERO);
		b.moveOrInterpolateTo(target, p.getYRot(), 0);
		if (b instanceof LivingEntity living) {
			living.lerpHeadTo(p.getYHeadRot(), 3);
		}
	}

	/** Walks (or hops) beside you on the ground and looks where you look when it stands still. */
	private void walk(ClientLevel level, LocalPlayer p, Kind k, double rightX, double rightZ, double backX, double backZ) {
		Entity b = buddy;
		double tx = p.getX() + rightX * 1.15 + backX * 0.7;
		double tz = p.getZ() + rightZ * 1.15 + backZ * 0.7;
		Vec3 cur = b.position();
		double dx = tx - cur.x;
		double dz = tz - cur.z;
		double dist = Math.sqrt(dx * dx + dz * dz);
		Double groundAtTarget = ground(level, tx, tz, p.getY() + 1.5);
		if (dist > 12 || groundAtTarget == null && Math.abs(cur.y - p.getY()) > 6) {
			teleport(b, new Vec3(tx, groundAtTarget != null ? groundAtTarget : p.getY(), tz), p.getYRot());
			return;
		}
		// walk towards the target: faster when far away, stop when close
		double step = dist < 0.3 ? 0 : Math.min(dist, Math.min(0.5, 0.06 + dist * 0.2));
		double nx = cur.x + (dist > 0 ? dx / dist * step : 0);
		double nz = cur.z + (dist > 0 ? dz / dist * step : 0);
		Double g = ground(level, nx, nz, Math.max(cur.y, p.getY()) + 1.2);
		double ny = g != null ? g : p.getY();
		// smooth steps up and down
		ny = cur.y + Mth.clamp(ny - cur.y, -0.6, 0.6);
		boolean moving = step > 0.02;
		if (k.motion() == Motion.HOP) {
			if (moving) {
				hopPhase += 0.32F;
			} else {
				hopPhase = 0;
			}
			double hop = Math.abs(Math.sin(hopPhase)) * 0.35;
			ny += hop;
			b.setOnGround(hop < 0.03);
			if (moving && b instanceof net.minecraft.world.entity.animal.rabbit.Rabbit && --hopTicks <= 0) {
				// rabbits play their jump animation on entity event 1
				b.handleEntityEvent((byte) 1);
				hopTicks = 10;
			}
		} else {
			b.setOnGround(true);
		}
		float yaw = moving ? (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F : p.getYRot();
		b.setDeltaMovement(Vec3.ZERO);
		b.moveOrInterpolateTo(new Vec3(nx, ny, nz), yaw, 0);
		if (b instanceof LivingEntity living) {
			living.lerpHeadTo(moving ? yaw : p.getYHeadRot(), 3);
		}
	}

	/** Top of the first solid block below {@code fromY} at x/z (within 8 blocks), or null. */
	private static @Nullable Double ground(ClientLevel level, double x, double z, double fromY) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int startY = Mth.floor(fromY);
		for (int y = startY; y > startY - 8 && y >= level.getMinY(); y--) {
			pos.set(Mth.floor(x), y, Mth.floor(z));
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(level, pos);
			if (!shape.isEmpty()) {
				double top = y + shape.max(net.minecraft.core.Direction.Axis.Y);
				if (top <= fromY + 0.01) {
					return top;
				}
			}
		}
		return null;
	}

	private static void teleport(Entity b, Vec3 pos, float yaw) {
		if (b.getInterpolation() instanceof net.minecraft.world.entity.SteppedInterpolationHandler stepped) {
			stepped.cancel();
		}
		b.snapTo(pos.x, pos.y, pos.z, yaw, 0);
		b.setOldPosAndRot();
	}

	private void spawn(ClientLevel level, LocalPlayer p) {
		Kind k = kind();
		Entity e = k.type().create(level, EntitySpawnReason.COMMAND);
		if (e == null) {
			return;
		}
		e.setId(ENTITY_ID);
		if (k.baby() && e instanceof AgeableMob ageable) {
			ageable.setAge(-24000);
		}
		if (e instanceof LivingEntity living) {
			AttributeInstance size = living.getAttribute(Attributes.SCALE);
			if (size != null) {
				size.setBaseValue(k.scale());
			}
		}
		e.setNoGravity(true);
		e.setSilent(true);
		if (k.model() != null) {
			e.setInvisible(true);
		}
		e.noPhysics = true;
		double y = k.motion() == Motion.FLY ? p.getY() + 1.6 : p.getY();
		e.snapTo(p.getX(), y, p.getZ(), p.getYRot(), 0);
		level.addEntity(e);
		buddy = e;
		spawnedType = type.get();
		hopTicks = 0;
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
