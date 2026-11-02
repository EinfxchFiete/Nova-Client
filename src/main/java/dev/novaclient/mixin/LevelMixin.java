package dev.novaclient.mixin;

import dev.novaclient.module.visual.NoWeatherModule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
	/** Only the client world is affected; the integrated server keeps real weather. */
	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
	private void novaclient$noRain(float a, CallbackInfoReturnable<Float> cir) {
		if (NoWeatherModule.active() && ((Level) (Object) this).isClientSide()) {
			cir.setReturnValue(0.0F);
		}
	}
}
