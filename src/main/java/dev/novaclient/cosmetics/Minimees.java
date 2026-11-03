package dev.novaclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.novaclient.NovaClient;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Vector3f;

/**
 * Minimees: a small player (with the skin of any Minecraft name) as necklace, holding on to the pocket or
 * sitting on the head. Every mode has its own model instance with a fixed pose, because rendering is deferred
 * and a shared model would show the pose of the last player for everyone.
 */
public final class Minimees {
	/** Size of the mini player compared to a normal one. */
	public static final float SCALE = 0.25F;
	private static final Identifier CHAIN = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "textures/cosmetics/chain.png");

	private static final Map<String, PlayerModel> MODELS = new HashMap<>();
	private static final Map<String, ResolvableProfile> PROFILES = new HashMap<>();

	private Minimees() {
	}

	/** Skin of a player name (loaded in the background, the default skin until it is there). */
	public static PlayerSkin skin(String name) {
		Minecraft mc = Minecraft.getInstance();
		ResolvableProfile profile = PROFILES.computeIfAbsent(name.toLowerCase(java.util.Locale.ROOT), n -> ResolvableProfile.createUnresolved(name));
		return mc.playerSkinRenderCache().getOrDefault(profile).playerSkin();
	}

	private static PlayerModel model(String mode, boolean slim) {
		return MODELS.computeIfAbsent(mode + (slim ? ":slim" : ":wide"), k -> {
			ModelPart root = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
			PlayerModel m = new PlayerModel(root, slim);
			switch (mode) {
				case "head" -> {
					// sitting: legs forward, hands on the knees
					m.rightLeg.xRot = -1.45F;
					m.leftLeg.xRot = -1.45F;
					m.rightLeg.yRot = 0.12F;
					m.leftLeg.yRot = -0.12F;
					m.rightArm.xRot = -0.75F;
					m.leftArm.xRot = -0.75F;
				}
				case "pocket" -> {
					// holding on to the edge of the pocket with both hands, legs dangling
					m.rightArm.xRot = -2.9F;
					m.leftArm.xRot = -2.9F;
					m.rightArm.zRot = 0.25F;
					m.leftArm.zRot = -0.25F;
					m.rightLeg.xRot = 0.15F;
					m.leftLeg.xRot = -0.1F;
				}
				default -> {
					// necklace: hanging from the chain with both hands up
					m.rightArm.xRot = -3.0F;
					m.leftArm.xRot = -3.0F;
					m.rightArm.zRot = -0.35F;
					m.leftArm.zRot = 0.35F;
					m.rightLeg.zRot = 0.08F;
					m.leftLeg.zRot = -0.08F;
				}
			}
			return m;
		});
	}

	/**
	 * Renders the mini player. The pose stack must be in the space of the body part it belongs to
	 * (head for "head", body for "neck", right leg for "pocket"); {@code neckX/Y/Z} is where the mini's neck goes (model pixels).
	 */
	private static void mini(PoseStack pose, SubmitNodeCollector c, int light, String name, String mode, float neckX, float neckY, float neckZ, float yaw) {
		PlayerSkin skin = skin(name);
		PlayerModel model = model(mode, skin.model() == PlayerModelType.SLIM);
		pose.pushPose();
		pose.translate(neckX / 16F, neckY / 16F, neckZ / 16F);
		if (yaw != 0) {
			pose.rotate(Axis.YP, yaw);
		}
		pose.scale(SCALE, SCALE, SCALE);
		c.submitModelPart(model.root(), pose, RenderTypes.entityTranslucent(skin.body().texturePath()), light, OverlayTexture.NO_OVERLAY, null, 0xFFFFFFFF);
		pose.popPose();
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
	public static void neck(PoseStack pose, SubmitNodeCollector c, int light, String name, boolean chestArmor) {
		int gold = 0xFFE8C25A;
		pose.pushPose();
		if (chestArmor) {
			// stay outside of the chestplate
			pose.scale(1.2F, 1.0F, 1.45F);
		}
		Vector3f l = new Vector3f(-3.3F, 0.15F, -2.3F);
		Vector3f r = new Vector3f(3.3F, 0.15F, -2.3F);
		Vector3f lh = new Vector3f(-0.85F, 3.6F, -2.75F);
		Vector3f rh = new Vector3f(0.85F, 3.6F, -2.75F);
		chain(pose, c, light, l, lh, gold);
		chain(pose, c, light, r, rh, gold);
		chain(pose, c, light, new Vector3f(-3.3F, 0.15F, 2.2F), new Vector3f(3.3F, 0.15F, 2.2F), gold);
		chain(pose, c, light, new Vector3f(-3.3F, 0.15F, -2.3F), new Vector3f(-3.3F, 0.15F, 2.2F), gold);
		chain(pose, c, light, new Vector3f(3.3F, 0.15F, -2.3F), new Vector3f(3.3F, 0.15F, 2.2F), gold);
		pose.popPose();
		mini(pose, c, light, name, "neck", 0, 6.1F, chestArmor ? -3.7F : -2.75F, 0);
	}

	/** Holding on to the pocket of the right leg. Pose stack in right leg space. */
	public static void pocket(PoseStack pose, SubmitNodeCollector c, int light, String name) {
		mini(pose, c, light, name, "pocket", -1.1F, 3.3F, -2.7F, 0);
	}

	/** Sitting on the head; {@code top} is the y of the surface (model pixels, negative = up). Pose stack in head space. */
	public static void head(PoseStack pose, SubmitNodeCollector c, int light, String name, float top) {
		// sitting: the mini's hip (12 px below its neck) rests on the surface
		mini(pose, c, light, name, "head", 0, top - 12 * SCALE, 0.6F, 0);
	}
}
