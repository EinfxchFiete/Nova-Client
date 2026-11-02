package dev.novaclient.mixin;

import dev.novaclient.module.visual.FreelookModule;
import dev.novaclient.module.visual.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void novaclient$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
		float multiplier = ZoomModule.fovMultiplier();
		if (multiplier != 1.0F) {
			cir.setReturnValue(cir.getReturnValueF() * multiplier);
		}
	}

	/** Freelook: after the camera copied the player's rotation, replace it with the free camera angles. */
	@Inject(
		method = "alignWithEntity",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;", ordinal = 0)
	)
	private void novaclient$freelook(float partialTicks, CallbackInfo ci) {
		if (FreelookModule.isActive()) {
			setRotation(FreelookModule.yaw(), FreelookModule.pitch());
		}
	}
}
