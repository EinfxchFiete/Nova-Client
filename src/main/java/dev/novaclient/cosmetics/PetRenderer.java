package dev.novaclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.novaclient.module.cosmetics.CompanionModule;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws pets with their own model (converted Blockbench models, see PetModels) at the position of the invisible
 * companion entity that does the moving. Plays the model's own animations: idle when standing, walk (or fly) when
 * moving, pet while you sneak next to it, with smooth blending between them.
 */
public final class PetRenderer {
	private static int submits;
	private static int lastQuads;
	private static float moveBlend;
	private static float petBlend;
	private static long lastNanos;

	private PetRenderer() {
	}

	/** Number of pet models drawn in the last frame (tests). */
	public static int lastSubmits() {
		return submits;
	}

	/** Number of quads of the last drawn pet (tests). */
	public static int lastQuads() {
		return lastQuads;
	}

	/** Share of the walk/fly animation in the last frame (tests). */
	public static float moveBlend() {
		return moveBlend;
	}

	/** Size in blocks the model is drawn with: about half a block high, very tall models a bit taller. */
	public static float drawScale(PetModels.Model model) {
		return Mth.clamp(9.0F / Math.max(1, model.height), 0.35F, 0.85F) / 16.0F;
	}

	public static void submit(LevelRenderContext ctx) {
		submits = 0;
		CompanionModule m = CompanionModule.get();
		Minecraft mc = Minecraft.getInstance();
		if (m == null || !m.isEnabled() || mc.level == null || mc.player == null) {
			return;
		}
		Entity pet = m.getBuddy();
		String name = m.customModel();
		if (pet == null || name == null || pet.isRemoved()) {
			return;
		}
		PetModels.Model model = PetModels.get(name);
		if (model == null) {
			return;
		}
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 pos = pet.getPosition(pt).subtract(ctx.levelState().cameraRenderState.pos);
		float yaw = pet instanceof LivingEntity living ? Mth.rotLerp(pt, living.yBodyRotO, living.yBodyRot) : Mth.rotLerp(pt, pet.yRotO, pet.getYRot());
		float seconds = (pet.tickCount + pt) / 20.0F;

		// smooth switch between the animations (frame time based, so it does not depend on the frame rate)
		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0 : Math.min(0.1F, (now - lastNanos) / 1.0E9F);
		lastNanos = now;
		double speed = pet.position().subtract(pet.xo, pet.yo, pet.zo).horizontalDistance();
		boolean moving = speed > 0.015;
		boolean petting = !moving && mc.player.isShiftKeyDown() && mc.player.distanceTo(pet) < 4 && model.hasAnimation("pet");
		moveBlend = approach(moveBlend, moving ? 1 : 0, dt * 5);
		petBlend = approach(petBlend, petting ? 1 : 0, dt * 4);
		String move = m.flies() && model.hasAnimation("fly") ? "fly" : "walk";
		float idle = (1 - moveBlend) * (1 - petBlend);
		Matrix4f[] bones = model.pose(new String[] {"idle", move, "pet"}, new float[] {seconds, seconds, seconds},
			new float[] {idle, moveBlend * (1 - petBlend), petBlend});

		// all vertices are computed now: rendering happens later (submit system)
		int nq = model.quadCount();
		float[] v = new float[nq * 12];
		float[] n = new float[nq * 3];
		Vector3f p = new Vector3f();
		for (int q = 0; q < nq; q++) {
			Matrix4f b = bones[model.quadBone[q]];
			for (int k = 0; k < 4; k++) {
				int src = q * 20 + k * 5;
				b.transformPosition(model.quads[src], model.quads[src + 1], model.quads[src + 2], p);
				v[q * 12 + k * 3] = p.x;
				v[q * 12 + k * 3 + 1] = p.y;
				v[q * 12 + k * 3 + 2] = p.z;
			}
			// normal from the corners (counter-clockwise seen from the front)
			int o = q * 12;
			float ax = v[o + 3] - v[o], ay = v[o + 4] - v[o + 1], az = v[o + 5] - v[o + 2];
			float bx = v[o + 9] - v[o], by = v[o + 10] - v[o + 1], bz = v[o + 11] - v[o + 2];
			float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
			float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
			if (len < 1e-6F) {
				nx = 0;
				ny = 1;
				nz = 0;
				len = 1;
			}
			// the corners go u0v0, u0v1, u1v1, u1v0: (c1-c0) x (c3-c0) points out of the front side
			n[q * 3] = nx / len;
			n[q * 3 + 1] = ny / len;
			n[q * 3 + 2] = nz / len;
		}
		int frame = model.frame(pet.tickCount);
		float frames = model.frames;
		float[] uv = model.quads;

		PoseStack pose = ctx.poseStack();
		pose.pushPose();
		pose.translate(pos.x, pos.y, pos.z);
		pose.rotate(Axis.YP, (180.0F - yaw) * Mth.DEG_TO_RAD);
		float s = drawScale(model);
		pose.scale(s, s, s);
		if (!m.flies()) {
			pose.translate(0, -model.minY, 0); // feet on the ground
		}
		Identifier texture = Identifier.fromNamespaceAndPath("novaclient", "textures/pets/" + name + ".png");
		RenderType type = model.translucent ? RenderTypes.entityTranslucentCull(texture) : RenderTypes.entityCutoutCull(texture);
		int light = mc.getEntityRenderDispatcher().getRenderer(pet).getPackedLightCoords(pet, pt);
		ctx.submitNodeCollector().submitCustomGeometry(pose, type, (pp, buffer) -> {
			for (int q = 0; q < nq; q++) {
				for (int k = 0; k < 4; k++) {
					int i = q * 12 + k * 3;
					int t = q * 20 + k * 5;
					buffer.addVertex(pp, v[i], v[i + 1], v[i + 2]).setColor(-1).setUv(uv[t + 3], (frame + uv[t + 4]) / frames)
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pp, n[q * 3], n[q * 3 + 1], n[q * 3 + 2]);
				}
			}
		});
		pose.popPose();
		lastQuads = nq;
		submits++;
	}

	private static float approach(float value, float target, float step) {
		return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
	}
}
