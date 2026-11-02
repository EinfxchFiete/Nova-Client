package dev.novaclient.mixin;

import dev.novaclient.module.visual.OverlaysModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
	@Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void novaclient$hideOverlay(GuiGraphicsExtractor graphics, Identifier texture, float alpha, CallbackInfo ci) {
		if (OverlaysModule.hideTexture(texture)) {
			ci.cancel();
		}
	}

	@Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
	private void novaclient$hideVignette(GuiGraphicsExtractor graphics, Entity camera, CallbackInfo ci) {
		if (OverlaysModule.hideVignette()) {
			ci.cancel();
		}
	}
}
