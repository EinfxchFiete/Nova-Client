package dev.novaclient.mixin;

import dev.novaclient.module.visual.FpsBoostModule;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FPS-Boost: a lower particle limit per particle group (vanilla: 16384). */
@Mixin(ParticleGroup.class)
public abstract class ParticleGroupMixin<P extends Particle> {
	@Shadow
	@Final
	protected Queue<P> particles;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void novaclient$limit(Particle particle, CallbackInfoReturnable<Boolean> cir) {
		int limit = FpsBoostModule.particleLimit();
		if (limit > 0 && particles.size() >= limit) {
			cir.setReturnValue(false);
		}
	}
}
