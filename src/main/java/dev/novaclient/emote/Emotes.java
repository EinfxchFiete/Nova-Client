package dev.novaclient.emote;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.novaclient.module.cosmetics.EmotesModule;
import dev.novaclient.network.NovaNetwork;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Emotes: short player animations that other Nova players see too. */
public final class Emotes {
	public record Info(String name, long durationMs) {
	}

	public record Active(String id, long start) {
	}

	public static final Map<String, Info> ALL = new LinkedHashMap<>();
	/** Emotes that last until you move (sitting, sleeping). */
	private static final long LONG = 600_000;

	static {
		ALL.put("wave", new Info("Winken", 3000));
		ALL.put("cheer", new Info("Jubeln", 3000));
		ALL.put("dance", new Info("Tanzen", 6000));
		ALL.put("clap", new Info("Klatschen", 3000));
		ALL.put("facepalm", new Info("Facepalm", 2500));
		ALL.put("point", new Info("Zeigen", 2500));
		ALL.put("bow", new Info("Verbeugen", 2500));
		ALL.put("salute", new Info("Salutieren", 2500));
		ALL.put("zombie", new Info("Zombie", 4000));
		ALL.put("shrug", new Info("Schulterzucken", 2000));
		ALL.put("think", new Info("Nachdenken", 3000));
		ALL.put("headbang", new Info("Headbangen", 3000));
		ALL.put("airguitar", new Info("Luftgitarre", 4000));
		ALL.put("flex", new Info("Muskeln zeigen", 2500));
		ALL.put("panic", new Info("Panik", 3000));
		ALL.put("dab", new Info("Dab", 2500));
		ALL.put("sit", new Info("Hinsetzen", LONG));
		ALL.put("meditate", new Info("Meditieren", LONG));
		ALL.put("sleep", new Info("Schlafen", LONG));
		ALL.put("jumpingjack", new Info("Hampelmann", 4000));
		ALL.put("tpose", new Info("T-Pose", 3000));
		ALL.put("robot", new Info("Roboter", 5000));
		ALL.put("laugh", new Info("Lachen", 3000));
		ALL.put("cry", new Info("Weinen", 3500));
		ALL.put("disco", new Info("Disco", 5000));
		ALL.put("spin", new Info("Pirouette", 3000));
	}

	private static Active local;
	private static double startX;
	private static double startZ;
	private static CameraType previousCamera;

	private Emotes() {
	}

	public static String valid(String id) {
		return ALL.containsKey(id) ? id : "";
	}

	/** Starts an emote for the local player. */
	public static void play(String id) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || !ALL.containsKey(id)) {
			return;
		}
		local = new Active(id, System.currentTimeMillis());
		startX = p.getX();
		startZ = p.getZ();
		EmotesModule m = EmotesModule.get();
		if (m != null && m.switchCamera.isOn() && previousCamera == null && mc.options.getCameraType().isFirstPerson()) {
			previousCamera = mc.options.getCameraType();
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		}
		NovaNetwork.requestSync();
	}

	public static void stop() {
		local = null;
		Minecraft mc = Minecraft.getInstance();
		if (previousCamera != null) {
			mc.options.setCameraType(previousCamera);
			previousCamera = null;
		}
		NovaNetwork.requestSync();
	}

	/** Ends the local emote after its duration or when the player walks away. */
	public static void tick(Minecraft mc) {
		if (local == null) {
			return;
		}
		LocalPlayer p = mc.player;
		Info info = ALL.get(local.id());
		boolean expired = info == null || System.currentTimeMillis() - local.start() > info.durationMs();
		boolean moved = p == null || Math.abs(p.getX() - startX) + Math.abs(p.getZ() - startZ) > 0.35;
		if (expired || moved) {
			stop();
		}
	}

	public static Active localActive() {
		return local;
	}

	/** The emote of the player with this entity id (local or remote), or null. */
	private static Active forEntity(int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && entityId == mc.player.getId()) {
			return local;
		}
		if (mc.level == null) {
			return null;
		}
		Entity e = mc.level.getEntity(entityId);
		if (e instanceof AbstractClientPlayer player) {
			NovaNetwork.RemoteStatus s = NovaNetwork.status(player.getUUID());
			if (s != null && !s.emote().isEmpty()) {
				Info info = ALL.get(s.emote());
				if (info != null && System.currentTimeMillis() - s.emoteStart() < info.durationMs()) {
					return new Active(s.emote(), s.emoteStart());
				}
			}
		}
		return null;
	}

	/**
	 * Moves or turns the whole player (with cosmetics and cape) for emotes like sitting. Called at the end of the
	 * renderer's setupRotations: y is up (blocks), the player looks towards -z.
	 */
	public static void transform(AvatarRenderState state, PoseStack pose) {
		Active a = forEntity(state.id);
		if (a == null) {
			return;
		}
		float t = (System.currentTimeMillis() - a.start()) / 1000.0F;
		float in = Math.min(1.0F, t * 4.0F);
		float px = 0.9375F / 16.0F * state.scale; // one model pixel
		switch (a.id()) {
			case "sit" -> pose.translate(0, -10.0F * px * in, 0);
			case "meditate" -> pose.translate(0, (-10.0F + 5.0F * in + Mth.sin(t * 2.0F) * 1.0F) * px * in, 0);
			case "sleep" -> {
				pose.translate(0, 2.0F * px * in, 0);
				pose.rotate(Axis.XP, Mth.HALF_PI * in);
			}
			case "jumpingjack" -> pose.translate(0, Math.abs(Mth.sin(t * 6.0F)) * 3.5F * px, 0);
			case "laugh" -> pose.translate(0, Math.abs(Mth.sin(t * 16.0F)) * 0.6F * px, 0);
			case "spin" -> {
				// eases in and out: three turns in three seconds
				float p = Mth.clamp(t / 3.0F, 0, 1);
				float eased = p * p * (3 - 2 * p);
				pose.rotate(Axis.YP, eased * Mth.TWO_PI * 3);
			}
			default -> {
			}
		}
	}

	/** Called after the vanilla animation: overrides arms, legs and head while an emote plays. */
	public static void apply(PlayerModel m, AvatarRenderState state) {
		Active a = forEntity(state.id);
		if (a == null) {
			return;
		}
		float t = (System.currentTimeMillis() - a.start()) / 1000.0F;
		float in = Math.min(1.0F, t * 4.0F); // short blend-in
		switch (a.id()) {
			case "wave" -> {
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -2.75F);
				m.rightArm.yRot = 0;
				m.rightArm.zRot = 0.3F + Mth.sin(t * 10.0F) * 0.35F;
			}
			case "cheer" -> {
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -2.9F);
				m.leftArm.xRot = Mth.lerp(in, m.leftArm.xRot, -2.9F);
				m.rightArm.zRot = 0.25F + Mth.sin(t * 9.0F) * 0.2F;
				m.leftArm.zRot = -(0.25F + Mth.sin(t * 9.0F + Mth.PI) * 0.2F);
				m.head.xRot = -0.3F;
			}
			case "dance" -> {
				float b = t * 6.0F;
				m.rightArm.xRot = -2.6F * (0.5F + 0.5F * Mth.sin(b));
				m.leftArm.xRot = -2.6F * (0.5F + 0.5F * Mth.sin(b + Mth.PI));
				m.rightArm.zRot = 0.25F;
				m.leftArm.zRot = -0.25F;
				m.head.zRot = Mth.sin(b) * 0.2F;
				m.rightLeg.xRot = Mth.sin(b) * 0.35F;
				m.leftLeg.xRot = -Mth.sin(b) * 0.35F;
			}
			case "clap" -> {
				float gap = 0.45F * Math.abs(Mth.cos(t * 8.0F));
				m.rightArm.xRot = -1.35F;
				m.leftArm.xRot = -1.35F;
				m.rightArm.yRot = -0.55F + gap;
				m.leftArm.yRot = 0.55F - gap;
				m.rightArm.zRot = 0;
				m.leftArm.zRot = 0;
			}
			case "facepalm" -> {
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -2.25F);
				m.rightArm.yRot = -0.55F;
				m.rightArm.zRot = 0;
				m.head.xRot = 0.45F;
				m.head.yRot = Mth.sin(t * 3.0F) * 0.12F;
			}
			case "point" -> {
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -1.55F);
				m.rightArm.yRot = -0.1F;
				m.rightArm.zRot = 0;
			}
			case "bow" -> {
				// head down, one hand on the chest, the other arm to the side
				float d = in * (0.5F + 0.5F * Mth.sin(Math.min(t, 2.0F) * Mth.PI / 2.0F));
				m.head.xRot = 0.95F * d;
				m.rightArm.xRot = -1.05F * d;
				m.rightArm.yRot = 0.75F * d;
				m.rightArm.zRot = 0;
				m.leftArm.xRot = 0.35F * d;
				m.leftArm.zRot = -0.35F * d;
			}
			case "salute" -> {
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -2.35F);
				m.rightArm.yRot = -0.75F;
				m.rightArm.zRot = 0.35F;
				m.head.xRot = -0.1F;
			}
			case "zombie" -> {
				float sway = Mth.sin(t * 4.0F) * 0.08F;
				m.rightArm.xRot = -1.55F + sway;
				m.leftArm.xRot = -1.55F - sway;
				m.rightArm.yRot = 0;
				m.leftArm.yRot = 0;
				m.rightArm.zRot = 0;
				m.leftArm.zRot = 0;
				m.head.zRot = Mth.sin(t * 2.0F) * 0.15F;
				m.head.xRot = 0.15F;
			}
			case "shrug" -> {
				float up = in * (0.5F + 0.5F * Mth.sin(t * 6.0F));
				m.rightArm.xRot = -0.55F;
				m.leftArm.xRot = -0.55F;
				m.rightArm.zRot = 0.6F + up * 0.25F;
				m.leftArm.zRot = -0.6F - up * 0.25F;
				m.head.zRot = 0.2F * in;
			}
			case "think" -> {
				// hand on the chin, looking around
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -2.0F);
				m.rightArm.yRot = -0.6F;
				m.rightArm.zRot = 0;
				m.leftArm.xRot = -0.6F;
				m.leftArm.yRot = 0.7F;
				m.head.yRot = Mth.sin(t * 1.5F) * 0.35F;
				m.head.xRot = -0.15F;
			}
			case "headbang" -> {
				// nodding hard to the beat, fists up
				float beat = Mth.sin(t * 13.0F);
				m.head.xRot = in * (0.25F + beat * 0.45F);
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -2.6F + beat * 0.15F);
				m.rightArm.zRot = 0.35F;
				m.leftArm.xRot = Mth.lerp(in, m.leftArm.xRot, -2.6F - beat * 0.15F);
				m.leftArm.zRot = -0.35F;
			}
			case "airguitar" -> {
				// left hand on the neck, right hand strumming
				m.leftArm.xRot = Mth.lerp(in, m.leftArm.xRot, -1.2F);
				m.leftArm.yRot = 0.55F;
				m.leftArm.zRot = -0.2F;
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -0.75F + Mth.sin(t * 18.0F) * 0.35F);
				m.rightArm.yRot = -0.45F;
				m.rightArm.zRot = 0.15F;
				m.head.xRot = 0.2F + Mth.sin(t * 9.0F) * 0.12F;
				m.head.zRot = Mth.sin(t * 4.5F) * 0.15F;
				m.rightLeg.xRot = Math.max(0, Mth.sin(t * 9.0F)) * -0.3F;
			}
			case "flex" -> {
				// both arms up and bent like a bodybuilder, pumping
				float pump = 0.5F + 0.5F * Mth.sin(t * 7.0F);
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -1.55F);
				m.rightArm.zRot = 1.25F + pump * 0.2F;
				m.leftArm.xRot = Mth.lerp(in, m.leftArm.xRot, -1.55F);
				m.leftArm.zRot = -(1.25F + pump * 0.2F);
				m.head.yRot = Mth.sin(t * 2.0F) * 0.3F;
			}
			case "panic" -> {
				// arms flailing, looking around
				m.rightArm.xRot = -2.4F + Mth.sin(t * 16.0F) * 0.9F;
				m.leftArm.xRot = -2.4F + Mth.sin(t * 16.0F + 2.1F) * 0.9F;
				m.rightArm.zRot = 0.4F + Mth.sin(t * 11.0F) * 0.3F;
				m.leftArm.zRot = -0.4F - Mth.sin(t * 11.0F + 1.0F) * 0.3F;
				m.head.yRot = Mth.sin(t * 8.0F) * 0.6F;
				m.rightLeg.xRot = Mth.sin(t * 14.0F) * 0.5F;
				m.leftLeg.xRot = -Mth.sin(t * 14.0F) * 0.5F;
			}
			case "dab" -> {
				// left arm stretched up to the side, right arm across the face, head tucked in
				m.leftArm.xRot = 0;
				m.leftArm.yRot = 0;
				m.leftArm.zRot = Mth.lerp(in, 0, -2.2F);
				m.rightArm.xRot = Mth.lerp(in, m.rightArm.xRot, -1.75F);
				m.rightArm.yRot = -1.0F * in;
				m.rightArm.zRot = 0;
				m.head.xRot = 0.5F * in;
				m.head.yRot = 0.35F * in;
			}
			case "sit" -> {
				sitLegs(m, in, 0.3F);
				m.rightArm.xRot = -0.55F * in;
				m.leftArm.xRot = -0.55F * in;
				m.rightArm.zRot = 0;
				m.leftArm.zRot = 0;
			}
			case "meditate" -> {
				// cross-legged, hands on the knees, floating gently
				sitLegs(m, in, 0.6F);
				m.rightArm.xRot = -0.75F * in;
				m.leftArm.xRot = -0.75F * in;
				m.rightArm.yRot = 0;
				m.leftArm.yRot = 0;
				m.rightArm.zRot = 0.55F * in;
				m.leftArm.zRot = -0.55F * in;
				m.head.xRot = 0.12F;
				m.head.yRot = 0;
			}
			case "sleep" -> {
				m.rightArm.xRot = 0;
				m.leftArm.xRot = 0;
				m.rightArm.zRot = 0.12F;
				m.leftArm.zRot = -0.12F;
				m.rightLeg.xRot = 0;
				m.leftLeg.xRot = 0;
				m.head.xRot = 0;
				m.head.yRot = 0;
				m.head.zRot = 0.25F + Mth.sin(t * 1.4F) * 0.04F;
			}
			case "jumpingjack" -> {
				float open = 0.5F - 0.5F * Mth.cos(t * 12.0F);
				m.rightArm.xRot = 0;
				m.leftArm.xRot = 0;
				m.rightArm.yRot = 0;
				m.leftArm.yRot = 0;
				m.rightArm.zRot = 0.2F + open * 2.5F;
				m.leftArm.zRot = -(0.2F + open * 2.5F);
				m.rightLeg.xRot = 0;
				m.leftLeg.xRot = 0;
				m.rightLeg.zRot = open * 0.45F;
				m.leftLeg.zRot = -open * 0.45F;
			}
			case "tpose" -> {
				m.rightArm.xRot = 0;
				m.leftArm.xRot = 0;
				m.rightArm.yRot = 0;
				m.leftArm.yRot = 0;
				m.rightArm.zRot = Mth.HALF_PI * in;
				m.leftArm.zRot = -Mth.HALF_PI * in;
				m.head.xRot = 0;
				m.head.yRot = 0;
			}
			case "robot" -> {
				// stiff, stepped movements
				int step = (int) (t * 3.0F);
				boolean alt = step % 2 == 0;
				m.rightArm.xRot = alt ? -Mth.HALF_PI : 0;
				m.leftArm.xRot = alt ? 0 : -Mth.HALF_PI;
				m.rightArm.yRot = 0;
				m.leftArm.yRot = 0;
				m.rightArm.zRot = 0;
				m.leftArm.zRot = 0;
				m.head.xRot = 0;
				m.head.yRot = (step % 4 - 1.5F) * 0.4F;
				m.rightLeg.xRot = alt ? -0.35F : 0;
				m.leftLeg.xRot = alt ? 0 : -0.35F;
			}
			case "laugh" -> {
				// hands on the belly, head thrown back, shaking
				float shake = Mth.sin(t * 16.0F);
				m.rightArm.xRot = -0.9F;
				m.leftArm.xRot = -0.9F;
				m.rightArm.yRot = -0.65F;
				m.leftArm.yRot = 0.65F;
				m.rightArm.zRot = 0;
				m.leftArm.zRot = 0;
				m.head.xRot = -0.45F + shake * 0.1F;
				m.head.zRot = shake * 0.06F;
			}
			case "cry" -> {
				// hands in front of the eyes, head down, sobbing
				float sob = Mth.sin(t * 9.0F);
				m.rightArm.xRot = -2.35F + sob * 0.06F;
				m.leftArm.xRot = -2.35F - sob * 0.06F;
				m.rightArm.yRot = -0.5F;
				m.leftArm.yRot = 0.5F;
				m.rightArm.zRot = 0;
				m.leftArm.zRot = 0;
				m.head.xRot = 0.4F + Math.abs(sob) * 0.08F;
				m.head.yRot = 0;
			}
			case "disco" -> {
				// pointing up to the side and down across the body in turns, other hand on the hip
				boolean up = (int) (t * 2.0F) % 2 == 0;
				float beat = Mth.sin(t * Mth.TWO_PI * 2.0F);
				m.rightArm.xRot = up ? -2.6F : -0.7F;
				m.rightArm.yRot = up ? 0 : -0.9F;
				m.rightArm.zRot = up ? 0.75F : 0;
				m.leftArm.xRot = 0.15F;
				m.leftArm.yRot = 0;
				m.leftArm.zRot = -0.55F;
				m.head.zRot = beat * 0.12F;
				m.head.yRot = up ? -0.35F : 0.3F;
				m.rightLeg.zRot = 0.12F + beat * 0.08F;
				m.leftLeg.zRot = -0.12F + beat * 0.08F;
			}
			case "spin" -> {
				// arms rounded above the head like a dancer
				m.rightArm.xRot = -2.9F * in;
				m.leftArm.xRot = -2.9F * in;
				m.rightArm.yRot = 0;
				m.leftArm.yRot = 0;
				m.rightArm.zRot = -0.35F * in;
				m.leftArm.zRot = 0.35F * in;
				m.rightLeg.xRot = 0;
				m.leftLeg.xRot = 0;
			}
			default -> {
			}
		}
	}

	private static void sitLegs(PlayerModel m, float in, float spread) {
		m.rightLeg.xRot = -1.42F * in;
		m.leftLeg.xRot = -1.42F * in;
		m.rightLeg.yRot = spread * in;
		m.leftLeg.yRot = -spread * in;
		m.rightLeg.zRot = 0;
		m.leftLeg.zRot = 0;
	}
}
