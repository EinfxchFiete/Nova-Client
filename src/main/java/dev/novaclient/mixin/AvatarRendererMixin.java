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
		novaclient$nameIcon(state, poseStack, collector, camera);
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

	/** The own name icon: drawn over the spaces that {@link dev.novaclient.cosmetics.Badges} put in front of the name. */
	private static void novaclient$nameIcon(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || state.id != mc.player.getId() || !state.nameTag.getString().startsWith(" ")) {
			return;
		}
		dev.novaclient.cosmetics.NameIcons.Icon icon = dev.novaclient.module.cosmetics.NametagModule.activeIcon();
		if (icon == null) {
			return;
		}
		int frame = dev.novaclient.module.cosmetics.NametagModule.iconFrame(icon);
		float x0 = -mc.font.width(state.nameTag) / 2.0F;
		float x1 = x0 + icon.width();
		float y0 = (state.showExtraEars ? -10 : 0) - 0.5F;
		float y1 = y0 + dev.novaclient.cosmetics.NameIcons.SIZE;
		float v0 = frame / (float) icon.frames();
		float v1 = (frame + 1) / (float) icon.frames();
		var att = state.nameTagAttachment;
		poseStack.pushPose();
		poseStack.translate(att.x, att.y + 0.5, att.z);
		poseStack.rotate(camera.orientation);
		poseStack.scale(0.025F, -0.025F, 0.025F);
		int color = state.isDiscrete ? 0x60FFFFFF : -1;
		// the normal points straight up (world space), where the level light is brightest: the picture keeps its real colours
		collector.submitCustomGeometry(poseStack, state.isDiscrete ? RenderTypes.text(icon.texture()) : RenderTypes.textSeeThrough(icon.texture()), (pose, buffer) -> {
			int light = LightCoordsUtil.FULL_BRIGHT;
			float z = -0.4F; // just in front of the name tag background (towards the camera)
			buffer.addVertex(pose, x0, y0, z).setColor(color).setUv(0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
			buffer.addVertex(pose, x0, y1, z).setColor(color).setUv(0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
			buffer.addVertex(pose, x1, y1, z).setColor(color).setUv(1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
			buffer.addVertex(pose, x1, y0, z).setColor(color).setUv(1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
		});
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
