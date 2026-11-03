package dev.novaclient.mixin;

import dev.novaclient.util.ItemPhysicsState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.novaclient.module.visual.ItemPhysicsModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item physics: items that lie on the ground are drawn flat (flat items) or resting (blocks) with a fixed rotation. */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
	@Shadow
	@Final
	private RandomSource random;

	protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
		at = @At("TAIL"))
	private void novaclient$groundFlag(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) {
		((ItemPhysicsState) state).novaclient$setOnGround(entity.onGround() && !entity.isInWater());
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
		+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("HEAD"), cancellable = true)
	private void novaclient$physics(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (!ItemPhysicsModule.active() || state.item.isEmpty() || !((ItemPhysicsState) state).novaclient$onGround()) {
			return;
		}
		ci.cancel();
		poseStack.pushPose();
		AABB box = state.item.getModelBoundingBox();
		boolean flat = box.maxZ - box.minZ < 0.2;
		// a fixed angle per item, so they don't all point the same way
		poseStack.rotate(Axis.YP, state.bobOffset * 3.7F);
		if (flat) {
			poseStack.translate(0.0F, (float) box.maxZ + 0.005F, 0.0F);
			poseStack.rotate(Axis.XP, (float) (Math.PI / 2));
		} else {
			poseStack.translate(0.0F, (float) -box.minY, 0.0F);
		}
		ItemEntityRenderer.submitMultipleFromCount(poseStack, collector, state.lightCoords, state, this.random, box);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
