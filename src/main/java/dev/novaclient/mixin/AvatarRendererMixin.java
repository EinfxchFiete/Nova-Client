package dev.novaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.novaclient.module.hud.MusicModule;
import dev.novaclient.music.CoverArt;
import dev.novaclient.music.NowPlaying;
import dev.novaclient.network.NovaNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	/** Size of the cover above the song line, in name tag pixels. */
	private static final float COVER = 20.0F;

	/** Above the name: the song this player is listening to (with time) and its cover. */
	@Inject(
		method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("TAIL")
	)
	private void novaclient$songLine(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (state.nameTag == null || state.nameTagAttachment == null) {
			return;
		}
		NowPlaying.Song song = songFor(state.id);
		if (song == null) {
			return;
		}
		String text = MusicModule.format(song);
		String time = MusicModule.showTimestamps() ? song.timestamp() : "";
		Component line = Component.literal("♪ ").withColor(0x1DB954).append(Component.literal(text).withColor(0xE8E8F0));
		if (!time.isEmpty()) {
			line = line.copy().append(Component.literal("  " + time).withColor(0x9AA0B4));
		}
		int lines = state.scoreText != null ? 2 : 1;
		float lineStep = 9.0F * 1.15F * 0.025F;
		poseStack.pushPose();
		poseStack.translate(0.0F, lines * lineStep, 0.0F);
		collector.submitNameTag(poseStack, state.nameTagAttachment, state.showExtraEars ? -10 : 0, line, !state.isDiscrete, state.lightCoords, camera);

		Identifier cover = MusicModule.showCovers() ? CoverArt.get(song.title(), song.artist()) : null;
		if (cover != null) {
			// same transform as the name tag (billboard facing the camera, y down, 1 unit = 1 text pixel)
			var att = state.nameTagAttachment;
			poseStack.pushPose();
			poseStack.translate(att.x, att.y + 0.5, att.z);
			poseStack.rotate(camera.orientation);
			poseStack.scale(0.025F, -0.025F, 0.025F);
			float half = COVER / 2.0F;
			float y1 = (state.showExtraEars ? -10 : 0) - 2.0F;
			float y0 = y1 - COVER;
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(cover), (pose, buffer) -> {
				int light = LightCoordsUtil.FULL_BRIGHT;
				buffer.addVertex(pose, -half, y0, 0).setColor(-1).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
				buffer.addVertex(pose, -half, y1, 0).setColor(-1).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
				buffer.addVertex(pose, half, y1, 0).setColor(-1).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
				buffer.addVertex(pose, half, y0, 0).setColor(-1).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
			});
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private static NowPlaying.Song songFor(int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && entityId == mc.player.getId()) {
			return MusicModule.songAboveName();
		}
		if (mc.level == null) {
			return null;
		}
		Entity e = mc.level.getEntity(entityId);
		return e instanceof AbstractClientPlayer p ? NovaNetwork.songInfo(p.getUUID()) : null;
	}
}
