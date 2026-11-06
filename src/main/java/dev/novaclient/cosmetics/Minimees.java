package dev.novaclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.novaclient.NovaClient;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Vector3f;

/**
 * Minimees: a small player (with the skin of any Minecraft name) as necklace, at the pocket, on the head, on the shoulder,
 * piggyback, held in the hand or floating next to you – optionally waving, dancing or kicking.
 *
 * Rendering is deferred, so a model that is posed differently per player would show the last pose for everyone.
 * Every (position, animation frame, slim) therefore gets its own model with a fixed pose; whole-body motion (pendulum
 * swing, bobbing) is done with the pose stack, which is copied at submit time.
 */
public final class Minimees {
	private static final Identifier CHAIN = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "textures/cosmetics/chain.png");
	/** Frames per animation loop. */
	private static final int FRAMES = 16;

	private static final Map<String, PlayerModel> MODELS = new HashMap<>();
	private static final Map<String, ResolvableProfile> PROFILES = new HashMap<>();

	private Minimees() {
	}

	public static float scale(String size) {
		return switch (size) {
			case "small" -> 0.19F;
			case "big" -> 0.32F;
			default -> 0.25F;
		};
	}

	/** Skin of a player name (loaded in the background, the default skin until it is there). */
	public static PlayerSkin skin(String name) {
		Minecraft mc = Minecraft.getInstance();
		ResolvableProfile profile = PROFILES.computeIfAbsent(name.toLowerCase(Locale.ROOT), n -> ResolvableProfile.createUnresolved(name));
		return mc.playerSkinRenderCache().getOrDefault(profile).playerSkin();
	}

	/** Number of cached models (tests). */
	public static int cachedModels() {
		return MODELS.size();
	}

	private static PlayerModel model(String mode, String anim, int frame, boolean slim) {
		return MODELS.computeIfAbsent(mode + ":" + anim + ":" + frame + (slim ? ":slim" : ":wide"), k -> {
			ModelPart root = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
			PlayerModel m = new PlayerModel(root, slim);
			pose(m, mode, anim, frame * Mth.TWO_PI / FRAMES);
			return m;
		});
	}

	private static boolean sitting(String mode) {
		return mode.equals("head") || mode.equals("shoulder");
	}

	private static boolean hanging(String mode) {
		return mode.equals("neck") || mode.equals("pocket") || mode.equals("hand");
	}

	/** Fixed pose of one frame. {@code t} goes once around (0..2π) per loop. */
	private static void pose(PlayerModel m, String mode, String anim, float t) {
		switch (mode) {
			case "head", "shoulder" -> {
				// sitting: legs forward, hands on the knees
				m.rightLeg.xRot = -1.45F;
				m.leftLeg.xRot = -1.45F;
				m.rightLeg.yRot = 0.12F;
				m.leftLeg.yRot = -0.12F;
				m.rightArm.xRot = -0.75F;
				m.leftArm.xRot = -0.75F;
			}
			case "back" -> {
				// piggyback: arms around your neck, legs around your waist
				m.rightArm.xRot = -1.55F;
				m.leftArm.xRot = -1.55F;
				m.rightArm.yRot = 0.35F;
				m.leftArm.yRot = -0.35F;
				m.rightLeg.xRot = -0.9F;
				m.leftLeg.xRot = -0.9F;
				m.rightLeg.zRot = 0.45F;
				m.leftLeg.zRot = -0.45F;
			}
			case "float" -> {
				// floating: arms a little out, legs relaxed
				m.rightArm.zRot = 0.45F;
				m.leftArm.zRot = -0.45F;
				m.rightLeg.xRot = 0.2F;
				m.leftLeg.xRot = 0.1F;
			}
			default -> {
				// neck, pocket and hand: hanging on with both hands up, legs dangling
				m.rightArm.xRot = -3.0F;
				m.leftArm.xRot = -3.0F;
				m.rightArm.zRot = mode.equals("neck") ? -0.35F : 0.2F;
				m.leftArm.zRot = mode.equals("neck") ? 0.35F : -0.2F;
				m.rightLeg.zRot = 0.08F;
				m.leftLeg.zRot = -0.08F;
			}
		}
		float s = Mth.sin(t);
		float c = Mth.cos(t);
		switch (anim) {
			case "wave" -> {
				if (!hanging(mode)) {
					// right arm up, waving from side to side
					m.rightArm.xRot = -2.75F;
					m.rightArm.yRot = 0;
					m.rightArm.zRot = 0.3F + s * 0.4F;
				} else {
					// hanging minimees hold on with one hand and wave with the other
					m.leftArm.xRot = -2.2F;
					m.leftArm.zRot = -0.4F - s * 0.35F;
				}
			}
			case "dance" -> {
				if (!hanging(mode) && !mode.equals("back")) {
					m.rightArm.xRot = -2.5F * (0.5F + 0.5F * s);
					m.leftArm.xRot = -2.5F * (0.5F - 0.5F * s);
					m.rightArm.zRot = 0.25F;
					m.leftArm.zRot = -0.25F;
				}
				m.head.zRot = s * 0.22F;
				m.head.xRot = Math.abs(c) * 0.15F;
				if (!sitting(mode)) {
					m.rightLeg.xRot += s * 0.35F;
					m.leftLeg.xRot -= s * 0.35F;
				}
			}
			case "kick" -> {
				// legs kick alternately (sitting: swinging the dangling legs)
				m.rightLeg.xRot += s * 0.55F;
				m.leftLeg.xRot -= s * 0.55F;
			}
			case "cheer" -> {
				if (!hanging(mode) && !mode.equals("back")) {
					// both arms up, pumping
					m.rightArm.xRot = -2.9F + Math.abs(s) * 0.35F;
					m.leftArm.xRot = -2.9F + Math.abs(s) * 0.35F;
					m.rightArm.zRot = 0.35F;
					m.leftArm.zRot = -0.35F;
				} else {
					// holding on: happy kicking and looking up
					m.rightLeg.xRot += Math.abs(s) * -0.5F;
					m.leftLeg.xRot += Math.abs(c) * -0.5F;
				}
				m.head.xRot = -0.3F;
			}
			case "nod" -> {
				// nodding to the beat (two nods per loop), swaying a little
				m.head.xRot = Math.abs(Mth.sin(t * 2.0F)) * 0.45F;
				m.head.zRot = s * 0.08F;
				m.body.zRot = s * 0.05F;
			}
			default -> {
				// a little breathing for everyone
				m.head.xRot += s * 0.04F;
			}
		}
	}

	private static int frame(AvatarRenderState state, float time) {
		// each player loops at its own phase
		return Math.floorMod((int) (time * 10.0F) + state.id * 7, FRAMES);
	}

	/**
	 * Renders the mini player with its neck at {@code neck} (model pixels, in the space of the body part it belongs to).
	 */
	private static void mini(PoseStack pose, SubmitNodeCollector c, int light, String name, String mode, String anim, int frame, float scale,
		Vector3f neck) {
		PlayerSkin skin = skin(name);
		PlayerModel model = model(mode, anim, frame, skin.model() == PlayerModelType.SLIM);
		pose.pushPose();
		pose.translate(neck.x / 16F, neck.y / 16F, neck.z / 16F);
		pose.scale(scale, scale, scale);
		c.submitModelPart(model.root(), pose, RenderTypes.entityTranslucent(skin.body().texturePath()), light, OverlayTexture.NO_OVERLAY, null, 0xFFFFFFFF);
		pose.popPose();
	}

	/** Where the hands of a hanging mini are, relative to its neck, in model pixels (arms straight up). */
	private static float handsAboveNeck(float scale) {
		return 7.9F * scale;
	}

	/** Pendulum swing around the point the mini hangs from: you walking makes it swing, standing it sways gently. */
	private static void swing(PoseStack pose, AvatarRenderState state, float time, Vector3f pivot, float strength) {
		float walk = Math.min(1.0F, state.walkAnimationSpeed);
		float fwd = Mth.sin(state.walkAnimationPos * 0.6662F) * 0.35F * walk + Mth.sin(time * 2.1F + state.id) * 0.05F;
		float side = Mth.sin(time * 1.6F + state.id * 0.7F) * 0.06F + Mth.cos(state.walkAnimationPos * 0.3331F) * 0.12F * walk;
		pose.translate(pivot.x / 16F, pivot.y / 16F, pivot.z / 16F);
		pose.rotate(Axis.XP, fwd * strength);
		pose.rotate(Axis.ZP, side * strength);
		pose.translate(-pivot.x / 16F, -pivot.y / 16F, -pivot.z / 16F);
	}

	/** A chain segment from a to b (model pixels). */
	private static void chain(PoseStack pose, SubmitNodeCollector c, int light, Vector3f a, Vector3f b, int color) {
		Vector3f d = new Vector3f(b).sub(a);
		float len = d.length();
		if (len < 0.01F) {
			return;
		}
		pose.pushPose();
		pose.translate(a.x / 16F, a.y / 16F, a.z / 16F);
		// rotate +y onto the direction
		pose.rotate(new org.joml.Quaternionf().rotationTo(new Vector3f(0, 1, 0), d.normalize()));
		pose.scale(0.35F, len, 0.35F);
		c.submitModelPart(CosmeticModels.chainLink, pose, RenderTypes.entityCutout(CHAIN), light, OverlayTexture.NO_OVERLAY, null, color);
		pose.popPose();
	}

	/** Necklace: chain around the neck, the mini hangs in front of the chest. Pose stack in body space. */
	public static void neck(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time, boolean chestArmor) {
		int gold = 0xFFE8C25A;
		float s = scale(l.minimeeSize());
		float z = chestArmor ? -3.7F : -2.75F;
		Vector3f hands = new Vector3f(0, 3.4F, z);
		pose.pushPose();
		if (chestArmor) {
			// stay outside of the chestplate
			pose.pushPose();
			pose.scale(1.2F, 1.0F, 1.45F);
		}
		chain(pose, c, light, new Vector3f(-3.3F, 0.15F, 2.2F), new Vector3f(3.3F, 0.15F, 2.2F), gold);
		chain(pose, c, light, new Vector3f(-3.3F, 0.15F, -2.3F), new Vector3f(-3.3F, 0.15F, 2.2F), gold);
		chain(pose, c, light, new Vector3f(3.3F, 0.15F, -2.3F), new Vector3f(3.3F, 0.15F, 2.2F), gold);
		if (chestArmor) {
			pose.popPose();
		}
		// the two chain ends come together at the mini's hands
		Vector3f lEnd = new Vector3f(chestArmor ? -3.96F : -3.3F, 0.15F, chestArmor ? -3.33F : -2.3F);
		Vector3f rEnd = new Vector3f(chestArmor ? 3.96F : 3.3F, 0.15F, chestArmor ? -3.33F : -2.3F);
		chain(pose, c, light, lEnd, new Vector3f(-0.7F, hands.y, hands.z), gold);
		chain(pose, c, light, rEnd, new Vector3f(0.7F, hands.y, hands.z), gold);
		// only the mini swings below its hands, the chain stays on the neck
		swing(pose, state, time, hands, 0.6F);
		mini(pose, c, light, l.minimee(), "neck", l.minimeeAnim(), frame(state, time), s, new Vector3f(0, hands.y + handsAboveNeck(s), hands.z));
		pose.popPose();
	}

	/** Holding on to the pocket of the right leg. Pose stack in right leg space. */
	public static void pocket(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		float s = scale(l.minimeeSize());
		Vector3f hands = new Vector3f(-1.1F, 1.2F, -2.75F);
		pose.pushPose();
		swing(pose, state, time, hands, 0.5F);
		mini(pose, c, light, l.minimee(), "pocket", l.minimeeAnim(), frame(state, time), s, new Vector3f(hands.x, hands.y + handsAboveNeck(s), hands.z));
		pose.popPose();
	}

	/** Hanging from your right hand (you hold its hands). Pose stack in right arm space. */
	public static void hand(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		float s = scale(l.minimeeSize());
		Vector3f hands = new Vector3f(-1.0F, 10.6F, -0.4F);
		pose.pushPose();
		swing(pose, state, time, hands, 1.0F);
		mini(pose, c, light, l.minimee(), "hand", l.minimeeAnim(), frame(state, time), s, new Vector3f(hands.x, hands.y + handsAboveNeck(s), hands.z));
		pose.popPose();
	}

	/** Sitting on the head; {@code top} is the y of the surface (model pixels, negative = up). Pose stack in head space. */
	public static void head(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time, float top) {
		float s = scale(l.minimeeSize());
		// sitting: the mini's hip (12 px below its neck) rests on the surface
		mini(pose, c, light, l.minimee(), "head", l.minimeeAnim(), frame(state, time), s, new Vector3f(0, top - 12 * s, 0.6F));
	}

	/** Sitting on your left shoulder, legs over the front. Pose stack in body space. */
	public static void shoulder(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time, boolean chestArmor) {
		float s = scale(l.minimeeSize());
		float top = chestArmor ? -1.1F : -0.1F;
		mini(pose, c, light, l.minimee(), "shoulder", l.minimeeAnim(), frame(state, time), s, new Vector3f(5.9F, top - 12 * s, 0.4F));
	}

	/** True if the vanilla cape layer draws a cape for this player (same checks as {@code CapeLayer}). */
	public static boolean capeVisible(AvatarRenderState state) {
		return !state.isInvisible && state.showCape && state.skin.cape() != null && !state.chestEquipment.is(net.minecraft.world.item.Items.ELYTRA);
	}

	/**
	 * Piggyback on your back, looking over your shoulder. Pose stack in body space. With a cape the mini rides on the
	 * outside of the cape (same pivot and swing as the vanilla cape model), otherwise it would be hidden under it.
	 */
	public static void back(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time, boolean chestArmor) {
		float s = scale(l.minimeeSize());
		float bag = l.extra().equals("backpack") ? 3.1F + (chestArmor ? 1.0F : 0.0F) : 0.0F;
		pose.pushPose();
		pose.translate(0, Mth.sin(state.walkAnimationPos * 0.6662F) * 0.25F * Math.min(1.0F, state.walkAnimationSpeed) / 16F, 0);
		if (capeVisible(state)) {
			// PlayerCapeModel: pivot (0, 0, 2), rotation X(6 + lean/2 + flap) Z(lean2/2); CapeLayer moves it out for chestplates
			pose.translate(0, chestArmor ? -0.053125F : 0, 2.0F / 16F + (chestArmor ? 0.06875F : 0));
			pose.rotate(Axis.XP, (6.0F + state.capeLean / 2.0F + state.capeFlap) * Mth.DEG_TO_RAD);
			pose.rotate(Axis.ZP, state.capeLean2 / 2.0F * Mth.DEG_TO_RAD);
			pose.rotate(Axis.YP, -state.capeLean2 / 2.0F * Mth.DEG_TO_RAD);
			// the cape is the slab z 0..1 here; hold on to its top edge, belly on the cape
			mini(pose, c, light, l.minimee(), "back", l.minimeeAnim(), frame(state, time), s, new Vector3f(-1.2F, 1.4F, 1.05F + 2 * s + bag * 0.4F));
		} else {
			float z = (chestArmor ? 3.2F : 2.3F) + 2 * s + bag;
			mini(pose, c, light, l.minimee(), "back", l.minimeeAnim(), frame(state, time), s, new Vector3f(-1.2F, 0.6F, z));
		}
		pose.popPose();
	}

	/** Floating behind your right shoulder, gently bobbing. Pose stack in the player's root space (not a body part). */
	public static void floating(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		float s = scale(l.minimeeSize());
		float bob = Mth.sin(time * 2.2F + state.id) * 1.2F;
		pose.pushPose();
		pose.translate(-11.0F / 16F, (-3.0F + bob) / 16F, 5.0F / 16F);
		pose.rotate(Axis.YP, Mth.sin(time * 0.8F) * 0.25F);
		pose.rotate(Axis.ZP, Mth.sin(time * 1.3F) * 0.06F);
		mini(pose, c, light, l.minimee(), "float", l.minimeeAnim(), frame(state, time), s, new Vector3f(0, 0, 0));
		pose.popPose();
	}
}
