package dev.novaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.novaclient.module.hud.MusicModule;
import dev.novaclient.network.NovaNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	/** One line above the name: the song this player is listening to. */
	@Inject(
		method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("TAIL")
	)
	private void novaclient$songLine(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (state.nameTag == null || state.nameTagAttachment == null) {
			return;
		}
		String song = songFor(state.id);
		if (song == null) {
			return;
		}
		Component line = Component.literal("♪ ").withColor(0x1DB954).append(Component.literal(song).withColor(0xE8E8F0));
		int lines = state.scoreText != null ? 2 : 1;
		poseStack.pushPose();
		poseStack.translate(0.0F, lines * 9.0F * 1.15F * 0.025F, 0.0F);
		collector.submitNameTag(poseStack, state.nameTagAttachment, state.showExtraEars ? -10 : 0, line, !state.isDiscrete, state.lightCoords, camera);
		poseStack.popPose();
	}

	private static String songFor(int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && entityId == mc.player.getId()) {
			return MusicModule.songAboveName();
		}
		if (mc.level == null) {
			return null;
		}
		Entity e = mc.level.getEntity(entityId);
		return e instanceof AbstractClientPlayer p ? NovaNetwork.song(p.getUUID()) : null;
	}
}
