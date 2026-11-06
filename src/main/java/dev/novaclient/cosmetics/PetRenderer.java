package dev.novaclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.novaclient.module.cosmetics.CompanionModule;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws pets with their own model (converted Blockbench models) at the position of the invisible companion entity
 * that does the moving. Animations: a little swimming wobble, the tail wags, the nose breathes.
 */
public final class PetRenderer {
	private static int submits;

	private PetRenderer() {
	}

	/** Number of pet models drawn in the last frame (tests). */
	public static int lastSubmits() {
		return submits;
	}

	public static void submit(LevelRenderContext ctx) {
		submits = 0;
		CompanionModule m = CompanionModule.get();
		Minecraft mc = Minecraft.getInstance();
		if (m == null || !m.isEnabled() || mc.level == null) {
			return;
		}
		Entity pet = m.getBuddy();
		String name = m.customModel();
		if (pet == null || name == null || pet.isRemoved()) {
			return;
		}
		ModelPart root = PetModels.get(name);
		if (root == null) {
			return;
		}
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 pos = pet.getPosition(pt).subtract(ctx.levelState().cameraRenderState.pos);
		float yaw = Mth.rotLerp(pt, pet.yRotO, pet.getYRot());
		float time = (pet.tickCount + pt) / 20.0F;
		double speed = pet.getDeltaMovement().horizontalDistance() + pet.position().subtract(pet.xo, pet.yo, pet.zo).horizontalDistance();
		float swim = (float) Math.min(1.0, speed * 6);

		// the shared parts are only used by this one pet, so they can be posed right before submitting
		root.getAllParts().forEach(ModelPart::resetPose);
		ModelPart tail = PetModels.find(root, "tail");
		ModelPart nose = PetModels.find(root, "nose");
		ModelPart torso = PetModels.find(root, "torso");
		if (tail != null) {
			tail.yRot = Mth.sin(time * (3.0F + swim * 6.0F)) * (0.12F + swim * 0.2F);
		}
		if (nose != null) {
			float breathe = 1 + 0.06F * (0.5F + 0.5F * Mth.sin(time * Mth.PI));
			nose.xScale = breathe;
			nose.yScale = breathe;
			nose.zScale = breathe;
		}
		if (torso != null) {
			torso.y += Mth.sin(time * Mth.PI) * 0.25F;
			torso.yScale = 1 + 0.03F * Mth.sin(time * Mth.PI);
		}

		PoseStack pose = ctx.poseStack();
		pose.pushPose();
		pose.translate(pos.x, pos.y, pos.z);
		pose.rotate(Axis.YP, (180.0F - yaw) * Mth.DEG_TO_RAD);
		// swimming in the air: tilts a little up and down
		pose.rotate(Axis.XP, Mth.sin(time * 1.7F) * 0.08F);
		pose.scale(-0.75F, -0.75F, 0.75F);
		Identifier texture = Identifier.fromNamespaceAndPath("novaclient", "textures/pets/" + name + ".png");
		int light = mc.getEntityRenderDispatcher().getRenderer(pet).getPackedLightCoords(pet, pt);
		ctx.submitNodeCollector().submitModelPart(root, pose, RenderTypes.entityCutout(texture), light, OverlayTexture.NO_OVERLAY, null, 0xFFFFFFFF);
		pose.popPose();
		submits++;
	}
}
