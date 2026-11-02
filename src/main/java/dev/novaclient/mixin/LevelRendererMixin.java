package dev.novaclient.mixin;

import dev.novaclient.module.visual.BlockOutlineModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	private static final String HIT_OUTLINE = "Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline("
		+ "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
		+ "Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V";

	/** ordinal 1 = the normal outline (ordinal 0 is the extra high-contrast pass). */
	@ModifyArg(method = "submitBlockOutline", at = @At(value = "INVOKE", target = HIT_OUTLINE, ordinal = 1), index = 4)
	private int novaclient$outlineColor(int color) {
		return BlockOutlineModule.active() ? BlockOutlineModule.color(color) : color;
	}

	@ModifyArg(method = "submitBlockOutline", at = @At(value = "INVOKE", target = HIT_OUTLINE, ordinal = 1), index = 5)
	private float novaclient$outlineWidth(float width) {
		return BlockOutlineModule.active() ? BlockOutlineModule.width(width) : width;
	}
}
