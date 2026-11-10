package dev.novaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.novaclient.emote.Emotes;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererEmoteMixin {
	/** Emotes that move the whole body (sitting, sleeping, spinning). */
	@Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
	private void novaclient$emoteTransform(AvatarRenderState state, PoseStack pose, float bodyRot, float scale, CallbackInfo ci) {
		Emotes.transform(state, pose);
	}
}
