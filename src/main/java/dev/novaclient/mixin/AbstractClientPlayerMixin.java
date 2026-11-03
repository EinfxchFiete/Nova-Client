package dev.novaclient.mixin;

import dev.novaclient.cosmetics.Loadout;
import dev.novaclient.module.cosmetics.CapeModule;
import dev.novaclient.network.NovaNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	/** Capes: your own from the Cape module, other Nova players' from the network. */
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void novaclient$cape(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (self == Minecraft.getInstance().player) {
			if (CapeModule.active()) {
				cir.setReturnValue(CapeModule.apply(cir.getReturnValue()));
			}
			return;
		}
		Loadout loadout = NovaNetwork.loadout(self.getUUID());
		if (loadout != null && !loadout.cape().isEmpty()) {
			cir.setReturnValue(CapeModule.withCape(cir.getReturnValue(), loadout.cape(), true));
		}
	}
}
