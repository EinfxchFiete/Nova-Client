package dev.novaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.novaclient.module.visual.LowFireModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
	@Unique
	private static boolean novaclient$pushed;

	@Inject(method = "submitFire", at = @At("HEAD"))
	private static void novaclient$lowFireStart(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite, CallbackInfo ci) {
		float offset = LowFireModule.activeOffset();
		novaclient$pushed = offset > 0.0F;
		if (novaclient$pushed) {
			poseStack.pushPose();
			poseStack.translate(0.0F, -offset, 0.0F);
		}
	}

	@Inject(method = "submitFire", at = @At("RETURN"))
	private static void novaclient$lowFireEnd(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (novaclient$pushed) {
			poseStack.popPose();
			novaclient$pushed = false;
		}
	}
}
