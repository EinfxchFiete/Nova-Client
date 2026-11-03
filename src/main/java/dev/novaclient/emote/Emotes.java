package dev.novaclient.emote;

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

	static {
		ALL.put("wave", new Info("Winken", 3000));
		ALL.put("cheer", new Info("Jubeln", 3000));
		ALL.put("dance", new Info("Tanzen", 6000));
		ALL.put("clap", new Info("Klatschen", 3000));
		ALL.put("facepalm", new Info("Facepalm", 2500));
		ALL.put("point", new Info("Zeigen", 2500));
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
			default -> {
			}
		}
	}
}
