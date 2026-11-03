package dev.novaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.novaclient.module.visual.HandSizeModule;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	/** Scales each hand around its own position (scaling around the eye would look the same). */
	@Inject(method = "submitArmWithItem", at = @At("HEAD"))
	private void novaclient$handSizeStart(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
		InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector collector,
		int lightCoords, CallbackInfo ci) {
		poseStack.pushPose();
		HandSizeModule m = HandSizeModule.active();
		if (m == null || playerState.avatarRenderState == null) {
			return;
		}
		HumanoidArm main = playerState.avatarRenderState.mainArm;
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
		float side = arm == HumanoidArm.RIGHT ? 1 : -1;
		float px = side * 0.56F;
		float py = -0.52F;
		float pz = -0.72F;
		poseStack.translate(px + side * m.offsetX.getFloat(), py + m.offsetY.getFloat(), pz);
		poseStack.scale(m.size.getFloat(), m.size.getFloat(), m.size.getFloat());
		poseStack.translate(-px, -py, -pz);
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void novaclient$handSizeEnd(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
		InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector collector,
		int lightCoords, CallbackInfo ci) {
		poseStack.popPose();
	}
}
