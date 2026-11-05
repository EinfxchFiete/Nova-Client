package dev.novaclient.music;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.novaclient.module.hud.LyricsModule;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the lyrics of {@link LyricsModule} as big floating text in the sky. The text hangs at a fixed height above
 * the horizon, a fixed distance from the camera (so it moves with you like the sun), and glides after your view
 * when you turn around. Without "through blocks" it is depth tested like normal world text.
 */
public final class LyricsSky {
	/** Distance from the camera in blocks; the apparent size does not depend on it. */
	private static final float DISTANCE = 30.0F;
	/** Degrees per font pixel at size 1.0. */
	private static final float DEGREES_PER_PIXEL = 0.45F;
	/** The text may be this wide at most (degrees), longer lines are shrunk. */
	private static final float MAX_DEGREES = 84.0F;
	/** Small head movements do not move the text. */
	private static final float DEAD_ZONE = 16.0F;

	private static float anchorYaw = Float.NaN;
	private static String anchorSong = "";
	private static long lastNanos;
	private static int lastSubmits;

	private LyricsSky() {
	}

	/** Number of text pieces submitted in the last frame (tests). */
	public static int lastSubmits() {
		return lastSubmits;
	}

	/** Current horizontal direction of the text (degrees, like an entity yaw); NaN if not shown. */
	public static float anchorYaw() {
		return anchorYaw;
	}

	public static void submit(LevelRenderContext ctx) {
		lastSubmits = 0;
		LyricsModule m = LyricsModule.get();
		if (m == null || !m.isEnabled() || !m.sky.isOn()) {
			anchorYaw = Float.NaN;
			return;
		}
		LyricsModule.Frame f = m.frame();
		if (f == null || f.alpha() < 0.01F) {
			anchorYaw = Float.NaN;
			return;
		}
		CameraRenderState cam = ctx.levelState().cameraRenderState;
		long nanos = System.nanoTime();
		float dt = lastNanos == 0 ? 0 : Math.min(0.25F, (nanos - lastNanos) / 1.0E9F);
		lastNanos = nanos;
		float camYaw = cam.yRot;
		if (Float.isNaN(anchorYaw) || !m.songKey().equals(anchorSong)) {
			anchorYaw = camYaw;
			anchorSong = m.songKey();
		} else if (m.follow.isOn()) {
			float diff = Mth.wrapDegrees(camYaw - anchorYaw);
			float outside = Math.abs(diff) - DEAD_ZONE;
			if (outside > 0) {
				anchorYaw = Mth.wrapDegrees(anchorYaw + Math.signum(diff) * outside * (1 - (float) Math.exp(-dt * 2.6F)));
			}
		}
		float time = (System.currentTimeMillis() % 3_600_000L) / 1000F;
		// a slow floating movement, like something hanging in the air
		float yaw = anchorYaw + Mth.sin(time * 0.35F) * 0.9F;
		float elevation = m.height.getFloat() + Mth.sin(time * 0.53F) * 0.6F;
		float size = m.size.getFloat();

		Quaternionf facing = new Quaternionf().rotationYXZ(Mth.PI - yaw * Mth.DEG_TO_RAD, elevation * Mth.DEG_TO_RAD, 0);
		Vector3f dir = facing.transform(new Vector3f(0, 0, -1));
		float k = DISTANCE * (float) Math.tan(DEGREES_PER_PIXEL * size * Mth.DEG_TO_RAD);

		PoseStack pose = ctx.poseStack();
		pose.pushPose();
		pose.translate(dir.x * DISTANCE, dir.y * DISTANCE, dir.z * DISTANCE);
		pose.rotate(facing);
		pose.scale(k, -k, k);
		Font font = Minecraft.getInstance().font;
		SubmitNodeCollector out = ctx.submitNodeCollector();
		Font.DisplayMode mode = m.throughBlocks.isOn() ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
		LyricsModule.Painter painter = new LyricsModule.Painter() {
			@Override
			public void draw(String s, float cx, float cy, float scale, int argb, boolean shadow) {
				if ((argb >>> 24) < 8 || scale <= 0.01F) {
					return;
				}
				// scaling keeps the translation of the pose, so every piece has the same distance to the camera and
				// the see-through pass draws them in the order they were submitted
				pose.pushPose();
				pose.scale(scale, scale, 1);
				out.submitText(pose, cx / scale - font.width(s) / 2F, cy / scale - 4F, FormattedCharSequence.forward(s, Style.EMPTY),
					shadow, mode, LightCoordsUtil.FULL_BRIGHT, argb, 0, 0);
				pose.popPose();
				lastSubmits++;
			}

			@Override
			public int width(String s) {
				return font.width(s);
			}
		};
		m.paint(f, painter, MAX_DEGREES / (DEGREES_PER_PIXEL * size) * Math.min(1, size));
		pose.popPose();
	}
}
