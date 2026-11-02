package dev.novaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.novaclient.module.visual.NoHurtCamModule;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void novaclient$noHurtCam(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
		// keep the vanilla death animation, only skip the damage tilt
		if (NoHurtCamModule.active() && !cameraState.entityRenderState.isDeadOrDying) {
			ci.cancel();
		}
	}
}
