package dev.novaclient.mixin;

import dev.novaclient.module.visual.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void novaclient$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
		float multiplier = ZoomModule.fovMultiplier();
		if (multiplier != 1.0F) {
			cir.setReturnValue(cir.getReturnValueF() * multiplier);
		}
	}
}
