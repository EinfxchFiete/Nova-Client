package dev.novaclient.mixin;

import dev.novaclient.module.visual.FreelookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Freelook: mouse movement turns the camera instead of the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void novaclient$freelookTurn(double xo, double yo, CallbackInfo ci) {
		if (FreelookModule.isActive() && (Object) this == Minecraft.getInstance().player) {
			FreelookModule.turn(xo, yo);
			ci.cancel();
		}
	}
}
