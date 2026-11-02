package dev.novaclient.mixin;

import dev.novaclient.module.cosmetics.NametagModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/** Own nametag in third person. */
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
	private void novaclient$ownNametag(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = Minecraft.getInstance();
		if (!cir.getReturnValueZ() && NametagModule.showOwn() && entity == mc.player
			&& !mc.options.getCameraType().isFirstPerson() && !mc.gui.hud.isHidden() && !entity.isInvisible()) {
			cir.setReturnValue(true);
		}
	}
}
