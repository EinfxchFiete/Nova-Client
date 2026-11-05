package dev.novaclient.mixin;

import dev.novaclient.module.visual.FpsBoostModule;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FPS-Boost: chests, signs, banners, ... further away than the chosen distance are skipped. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Shadow
	private Vec3 cameraPos;

	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private <E extends BlockEntity, S extends BlockEntityRenderState> void novaclient$distanceCull(E blockEntity, float partialTicks,
		ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean isGloballyRendered, CallbackInfoReturnable<S> cir) {
		double max = FpsBoostModule.blockEntityDistanceSq();
		// globally rendered ones (beacon beams, end gateways) stay
		if (max > 0 && !isGloballyRendered && cameraPos != null && Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(cameraPos) > max) {
			cir.setReturnValue(null);
		}
	}
}
