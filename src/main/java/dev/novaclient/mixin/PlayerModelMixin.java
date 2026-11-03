package dev.novaclient.mixin;

import dev.novaclient.emote.Emotes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	/** Emotes override the arm/head/leg animation after vanilla has set it up. */
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void novaclient$emotes(AvatarRenderState state, CallbackInfo ci) {
		Emotes.apply((PlayerModel) (Object) this, state);
	}
}
