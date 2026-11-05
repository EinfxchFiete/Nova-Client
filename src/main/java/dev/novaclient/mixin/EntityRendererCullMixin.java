package dev.novaclient.mixin;

import dev.novaclient.module.visual.FpsBoostModule;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FPS-Boost: entities beyond the chosen distance are not extracted for rendering at all. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullMixin<T extends Entity> {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void novaclient$distanceCull(T entity, Frustum culler, double camX, double camY, double camZ, float partialTicks,
		CallbackInfoReturnable<Boolean> cir) {
		if (!FpsBoostModule.shouldDraw(entity, camX, camY, camZ)) {
			cir.setReturnValue(false);
		}
	}
}
