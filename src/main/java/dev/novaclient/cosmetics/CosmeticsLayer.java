package dev.novaclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.novaclient.NovaClient;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

/** Renders head cosmetics and wings on players (yourself and other Nova players). */
public class CosmeticsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static Identifier tex(String name) {
		return Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "textures/cosmetics/" + name + ".png");
	}

	private static final Identifier BANDANA = tex("bandana");
	private static final Identifier CROWN = tex("crown");
	private static final Identifier TOPHAT = tex("tophat");
	private static final Identifier HORNS = tex("horns");
	private static final Identifier CATEARS = tex("catears");
	private static final Identifier HALO = tex("halo");

	public CosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible || state.isSpectator) {
			return;
		}
		Loadout loadout = Cosmetics.loadoutFor(state.id);
		if (loadout == null) {
			return;
		}
		CosmeticModels.ensureBuilt();
		float time = state.ageInTicks / 20.0F;
		if (!loadout.head().isEmpty()) {
			poseStack.pushPose();
			getParentModel().head.translateAndRotate(poseStack);
			head(poseStack, collector, light, state, loadout, time);
			poseStack.popPose();
		}
		if (!loadout.wings().isEmpty() && state.chestEquipment.getItem() != Items.ELYTRA) {
			poseStack.pushPose();
			getParentModel().body.translateAndRotate(poseStack);
			wings(poseStack, collector, light, state, loadout, time);
			poseStack.popPose();
		}
	}

	private static void part(SubmitNodeCollector c, ModelPart part, PoseStack pose, RenderType type, int light, int tint) {
		c.submitModelPart(part, pose, type, light, OverlayTexture.NO_OVERLAY, null, tint);
	}

	private void head(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		boolean helmet = !state.headEquipment.isEmpty();
		int tint = CosmeticRegistry.tint(l.headColor(), time);
		switch (l.head()) {
			case "bandana" -> {
				if (helmet) {
					return;
				}
				int color = l.headColor().isEmpty() || l.headColor().equals("Normal") ? 0xFFE53935 : tint;
				RenderType type = RenderTypes.entityCutout(BANDANA);
				part(c, CosmeticModels.bandana, pose, type, light, color);
				float swing = 0.35F + Math.min(1.0F, state.walkAnimationSpeed) * 0.9F;
				for (int i = 0; i < 2; i++) {
					pose.pushPose();
					pose.translate((i == 0 ? -0.9F : 0.9F) / 16F, -5.4F / 16F, 5.4F / 16F);
					pose.rotate(Axis.XP, swing + Mth.sin(time * 6.0F + i * 1.3F) * 0.12F);
					pose.rotate(Axis.ZP, (i == 0 ? -0.18F : 0.18F) + Mth.sin(time * 4.0F + i) * 0.05F);
					part(c, CosmeticModels.bandanaTail, pose, type, light, color);
					pose.popPose();
				}
			}
			case "crown" -> {
				if (!helmet) {
					pose.pushPose();
					pose.translate(0, Mth.sin(time * 2.0F) * 0.15F / 16F, 0);
					part(c, CosmeticModels.crown, pose, RenderTypes.entityCutout(CROWN), light, 0xFFFFFFFF);
					pose.popPose();
				}
			}
			case "tophat" -> {
				if (!helmet) {
					part(c, CosmeticModels.tophat, pose, RenderTypes.entityCutout(TOPHAT), light, 0xFFFFFFFF);
					int band = l.headColor().isEmpty() || l.headColor().equals("Normal") ? 0xFF8B5CF6 : tint;
					part(c, CosmeticModels.tophatBand, pose, RenderTypes.entityCutout(TOPHAT), light, band);
				}
			}
			case "horns" -> part(c, CosmeticModels.horns, pose, RenderTypes.entityCutout(HORNS), light,
				l.headColor().isEmpty() || l.headColor().equals("Normal") ? 0xFFFFFFFF : tint);
			case "catears" -> {
				if (helmet) {
					return;
				}
				int color = l.headColor().isEmpty() || l.headColor().equals("Normal") ? 0xFF3A3A44 : tint;
				for (int i = 0; i < 2; i++) {
					float side = i == 0 ? -1 : 1;
					float phase = (time + i * 1.7F) % 4.0F;
					float twitch = phase < 0.3F ? Mth.sin(phase / 0.3F * Mth.PI) * 0.45F : 0;
					pose.pushPose();
					pose.translate(side * 2.6F / 16F, -7.9F / 16F, -0.6F / 16F);
					pose.rotate(Axis.ZP, side * (0.28F + twitch));
					pose.rotate(Axis.XP, -0.12F);
					part(c, CosmeticModels.catEar, pose, RenderTypes.entityCutout(CATEARS), light, color);
					pose.popPose();
				}
			}
			case "halo" -> {
				pose.pushPose();
				pose.translate(0, (-11.6F + Mth.sin(time * 2.0F) * 0.4F) / 16F, 0);
				pose.rotate(Axis.YP, time * 0.8F);
				int color = l.headColor().isEmpty() || l.headColor().equals("Normal") ? 0xFFFFE27A : tint;
				part(c, CosmeticModels.halo, pose, RenderTypes.entityTranslucentEmissive(HALO), LightCoordsUtil.FULL_BRIGHT, color);
				pose.popPose();
			}
			default -> {
			}
		}
	}

	private void wings(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		Identifier texture = tex("wings_" + l.wings());
		boolean moving = state.walkAnimationSpeed > 0.15F;
		float speed = moving ? 5.5F : 2.2F;
		float amp = state.isCrouching ? 0.08F : moving ? 0.45F : 0.25F;
		float flap = (Mth.sin(time * speed) + 1.0F) / 2.0F * amp;
		int tint = l.wingsColor().isEmpty() || l.wingsColor().equals("Normal") ? 0xFFFFFFFF : CosmeticRegistry.tint(l.wingsColor(), time);
		RenderType type = RenderTypes.entityTranslucent(texture);
		for (int side = -1; side <= 1; side += 2) {
			pose.pushPose();
			pose.translate(side * 1.0F / 16F, 3.5F / 16F, 2.3F / 16F);
			pose.scale(side, 1, 1);
			pose.rotate(Axis.YP, -(0.55F + flap));
			pose.rotate(Axis.ZP, -0.18F);
			part(c, CosmeticModels.wing, pose, type, light, tint);
			pose.popPose();
		}
	}
}
