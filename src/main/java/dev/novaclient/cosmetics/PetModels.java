package dev.novaclient.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novaclient.NovaClient;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Pet models converted from Blockbench by tools/assets/bbmodel_pet.py (assets/novaclient/models/pets/&lt;name&gt;.json,
 * texture textures/pets/&lt;name&gt;.png). Model space is Blockbench's: pixels, y up, front = north (-z).
 * Bones carry the group pivots and rotations, quads are already placed in model space and move with their bone.
 * Animations (idle, walk, pet, fly) are Blockbench keyframes, evaluated like Blockbench does.
 */
public final class PetModels {
	private static final Map<String, Model> CACHE = new HashMap<>();

	private PetModels() {
	}

	/** One animation channel: keyframe times, values when arriving / leaving (3 each) and interpolation. */
	public record Channel(float[] times, float[] in, float[] out, byte[] interp) {
		static final byte LINEAR = 0, CATMULLROM = 1, STEP = 2;

		/** Value of axis a at time t (Blockbench BoneAnimator.interpolate, without loop wrapping). */
		float value(float t, int a, boolean loop) {
			int n = times.length;
			int before = -1, after = -1;
			for (int i = 0; i < n; i++) {
				if (times[i] < t) {
					before = i;
				} else if (after < 0) {
					after = i;
				}
			}
			float eps = 1 / 1200F;
			if (before >= 0 && Math.abs(times[before] - t) < eps) {
				return in[before * 3 + a];
			}
			if (after >= 0 && Math.abs(times[after] - t) < eps) {
				return in[after * 3 + a];
			}
			if (before >= 0 && interp[before] == STEP) {
				return out[before * 3 + a];
			}
			if (after < 0) {
				return before < 0 ? 0 : out[before * 3 + a];
			}
			if (before < 0) {
				return in[after * 3 + a];
			}
			float alpha = (t - times[before]) / Math.max(1e-6F, times[after] - times[before]);
			float p1 = out[before * 3 + a];
			float p2 = in[after * 3 + a];
			if (interp[before] == CATMULLROM || interp[after] == CATMULLROM) {
				int bp = before - 1;
				int ap = after + 1;
				if (loop && n >= 3) {
					if (bp < 0) {
						bp = n - 2;
					}
					if (ap >= n) {
						ap = 1;
					}
				}
				float p0 = bp >= 0 ? out[bp * 3 + a] : p1;
				float p3 = ap < n ? in[ap * 3 + a] : p2;
				// uniform Catmull-Rom like three.js SplineCurve
				float v0 = (p2 - p0) * 0.5F;
				float v1 = (p3 - p1) * 0.5F;
				float t2 = alpha * alpha;
				float t3 = t2 * alpha;
				return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * alpha + p1;
			}
			return p1 + (p2 - p1) * alpha;
		}
	}

	/** An animation: per bone the channels rotation (0), position (1), scale (2), or null. */
	public record Animation(float length, boolean loop, Channel[][] channels) {
		float time(float seconds) {
			if (length <= 0) {
				return 0;
			}
			return loop ? seconds % length : Math.min(seconds, length);
		}
	}

	public static final class Model {
		public final String name;
		public final int frames;
		public final int frameTicks;
		public final int @Nullable [] frameOrder;
		public final boolean translucent;
		/** Lowest point and height of what is visible in the idle pose (parts scaled to 0 by it are hidden). */
		public final float minY;
		public final float height;
		final int[] parent;
		final float[] origin;
		final float[] rotation;
		final String[] boneNames;
		final int[] quadBone;
		/** 4 vertices per quad: x, y, z, u, v. */
		final float[] quads;
		final Map<String, Animation> animations;

		Model(String name, JsonObject json) {
			this.name = name;
			JsonObject tex = json.getAsJsonObject("texture");
			frames = Math.max(1, tex.get("frames").getAsInt());
			frameTicks = Math.max(1, tex.get("frame_time").getAsInt());
			JsonElement order = tex.get("frame_order");
			if (order != null && order.isJsonArray() && !order.getAsJsonArray().isEmpty()) {
				JsonArray o = order.getAsJsonArray();
				frameOrder = new int[o.size()];
				for (int i = 0; i < o.size(); i++) {
					frameOrder[i] = Math.floorMod(o.get(i).getAsInt(), frames);
				}
			} else {
				frameOrder = null;
			}
			translucent = json.get("translucent").getAsBoolean();
			JsonArray bones = json.getAsJsonArray("bones");
			int nb = bones.size();
			parent = new int[nb];
			origin = new float[nb * 3];
			rotation = new float[nb * 3];
			boneNames = new String[nb];
			for (int i = 0; i < nb; i++) {
				JsonObject bo = bones.get(i).getAsJsonObject();
				boneNames[i] = bo.get("name").getAsString();
				parent[i] = bo.get("parent").getAsInt();
				if (parent[i] >= i) {
					throw new IllegalStateException("bones must come after their parent");
				}
				JsonArray o = bo.getAsJsonArray("origin");
				JsonArray r = bo.getAsJsonArray("rotation");
				for (int a = 0; a < 3; a++) {
					origin[i * 3 + a] = o.get(a).getAsFloat();
					rotation[i * 3 + a] = r.get(a).getAsFloat();
				}
			}

			JsonArray qs = json.getAsJsonArray("quads");
			quadBone = new int[qs.size()];
			quads = new float[qs.size() * 20];
			for (int i = 0; i < qs.size(); i++) {
				JsonArray q = qs.get(i).getAsJsonArray();
				quadBone[i] = q.get(0).getAsInt();
				for (int k = 0; k < 20; k++) {
					quads[i * 20 + k] = q.get(1 + k).getAsFloat();
				}
			}

			animations = new HashMap<>();
			JsonObject anims = json.getAsJsonObject("animations");
			for (Map.Entry<String, JsonElement> e : anims.entrySet()) {
				JsonObject an = e.getValue().getAsJsonObject();
				Channel[][] ch = new Channel[nb][3];
				for (Map.Entry<String, JsonElement> be : an.getAsJsonObject("bones").entrySet()) {
					int bone = Integer.parseInt(be.getKey());
					JsonObject chans = be.getValue().getAsJsonObject();
					String[] names = {"rotation", "position", "scale"};
					for (int c = 0; c < 3; c++) {
						if (chans.has(names[c])) {
							ch[bone][c] = channel(chans.getAsJsonArray(names[c]));
						}
					}
				}
				String loop = an.get("loop").getAsString();
				animations.put(e.getKey(), new Animation(an.get("length").getAsFloat(), loop.equals("loop"), ch));
			}

			Matrix4f[] rest = pose(new String[] {"idle"}, new float[] {0}, new float[] {1});
			float lo = Float.MAX_VALUE;
			float hi = -Float.MAX_VALUE;
			org.joml.Vector3f p = new org.joml.Vector3f();
			for (int q = 0; q < quadBone.length; q++) {
				Matrix4f m = rest[quadBone[q]];
				if (Math.abs(m.determinant3x3()) < 1e-6F) {
					continue;
				}
				for (int k = 0; k < 4; k++) {
					m.transformPosition(quads[q * 20 + k * 5], quads[q * 20 + k * 5 + 1], quads[q * 20 + k * 5 + 2], p);
					lo = Math.min(lo, p.y);
					hi = Math.max(hi, p.y);
				}
			}
			if (lo > hi) {
				JsonArray b = json.getAsJsonArray("bounds");
				lo = b.get(1).getAsFloat();
				hi = b.get(4).getAsFloat();
			}
			minY = lo;
			height = hi - lo;
		}

		private static Channel channel(JsonArray keys) {
			int n = keys.size();
			float[] times = new float[n];
			float[] in = new float[n * 3];
			float[] out = new float[n * 3];
			byte[] interp = new byte[n];
			for (int i = 0; i < n; i++) {
				JsonArray k = keys.get(i).getAsJsonArray();
				times[i] = k.get(0).getAsFloat();
				for (int a = 0; a < 3; a++) {
					in[i * 3 + a] = k.get(1 + a).getAsFloat();
					out[i * 3 + a] = k.get(4 + a).getAsFloat();
				}
				interp[i] = (byte) k.get(7).getAsInt();
			}
			return new Channel(times, in, out, interp);
		}

		public boolean hasAnimation(String anim) {
			return animations.containsKey(anim);
		}

		public int boneCount() {
			return parent.length;
		}

		public int quadCount() {
			return quadBone.length;
		}

		public int bone(String boneName) {
			for (int i = 0; i < boneNames.length; i++) {
				if (boneNames[i].equals(boneName)) {
					return i;
				}
			}
			return -1;
		}

		/** Texture frame (animated textures) at the given game tick. */
		public int frame(long ticks) {
			if (frames <= 1) {
				return 0;
			}
			long step = ticks / frameTicks;
			return frameOrder != null ? frameOrder[(int) (step % frameOrder.length)] : (int) (step % frames);
		}

		/**
		 * Bone matrices for a mix of animations: weights[i] is the share of anims[i] (missing animations count as rest pose).
		 * Rotation and position are blended linearly, scale too.
		 */
		public Matrix4f[] pose(String[] anims, float[] seconds, float[] weights) {
			int nb = parent.length;
			Matrix4f[] m = new Matrix4f[nb];
			float[] rot = new float[3];
			float[] pos = new float[3];
			float[] scl = new float[3];
			for (int b = 0; b < nb; b++) {
				for (int a = 0; a < 3; a++) {
					rot[a] = rotation[b * 3 + a];
					pos[a] = 0;
					scl[a] = 1;
				}
				for (int i = 0; i < anims.length; i++) {
					Animation an = animations.get(anims[i]);
					if (an == null || weights[i] <= 0) {
						continue;
					}
					Channel[] ch = an.channels()[b];
					float t = an.time(seconds[i]);
					float w = weights[i];
					for (int a = 0; a < 3; a++) {
						if (ch[0] != null) {
							rot[a] += w * ch[0].value(t, a, an.loop());
						}
						if (ch[1] != null) {
							pos[a] += w * ch[1].value(t, a, an.loop());
						}
						if (ch[2] != null) {
							scl[a] += w * (ch[2].value(t, a, an.loop()) - 1);
						}
					}
				}
				float ox = origin[b * 3];
				float oy = origin[b * 3 + 1];
				float oz = origin[b * 3 + 2];
				Matrix4f mb = parent[b] >= 0 ? new Matrix4f(m[parent[b]]) : new Matrix4f();
				mb.translate(ox + pos[0], oy + pos[1], oz + pos[2])
					.rotateZ(rot[2] * ((float) Math.PI / 180F))
					.rotateY(rot[1] * ((float) Math.PI / 180F))
					.rotateX(rot[0] * ((float) Math.PI / 180F))
					.scale(nz(scl[0]), nz(scl[1]), nz(scl[2]))
					.translate(-ox, -oy, -oz);
				m[b] = mb;
			}
			return m;
		}

		private static float nz(float s) {
			return Math.abs(s) < 1e-5F ? 1e-5F : s;
		}
	}

	/** The model, or null if it could not be loaded. */
	public static synchronized @Nullable Model get(String name) {
		if (CACHE.containsKey(name)) {
			return CACHE.get(name);
		}
		Model model = null;
		try (InputStream in = PetModels.class.getResourceAsStream("/assets/novaclient/models/pets/" + name + ".json")) {
			if (in != null) {
				model = new Model(name, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not load pet model {}", name, e);
		}
		CACHE.put(name, model);
		return model;
	}
}
