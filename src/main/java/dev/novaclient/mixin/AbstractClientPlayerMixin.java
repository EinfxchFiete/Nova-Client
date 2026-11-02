package dev.novaclient.mixin;

import dev.novaclient.module.cosmetics.CapeModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	/** Cape cosmetic: only for your own player. */
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void novaclient$cape(CallbackInfoReturnable<PlayerSkin> cir) {
		if (CapeModule.active() && (Object) this == Minecraft.getInstance().player) {
			cir.setReturnValue(CapeModule.apply(cir.getReturnValue()));
		}
	}
}
