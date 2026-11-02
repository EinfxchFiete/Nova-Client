package dev.novaclient.mixin;

import dev.novaclient.module.visual.FullbrightModule;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("TAIL"))
	private void novaclient$fullbright(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
		if (FullbrightModule.active() && renderState.needsUpdate) {
			renderState.nightVisionEffectIntensity = 1.0F;
			renderState.brightness = 1.0F;
		}
	}
}
