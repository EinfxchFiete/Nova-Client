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
	private static final Identifier HORNS_TINT = tex("horns_tint");
	private static final Identifier CROWN_GEMS = tex("crown_gems");
	private static final Identifier PARTYHAT = tex("partyhat");
	private static final Identifier COWBOYHAT = tex("cowboyhat");
	private static final Identifier BUNNYEARS = tex("bunnyears");
	private static final Identifier HEADPHONES = tex("headphones");
	private static final Identifier SUNGLASSES = tex("sunglasses");
	private static final Identifier VIKING = tex("vikinghelmet");
	private static final Identifier FLOWERCROWN = tex("flowercrown");
	private static final Identifier BEANIE = tex("beanie");
	private static final Identifier BACKPACK = tex("backpack");
	private static final Identifier TAILS = tex("tails");
	private static final Identifier SCARF = tex("scarf");

	/** Chosen colour, or the cosmetic's own default when "Normal". */
	private static int color(String name, float time, int normal) {
		return name.isEmpty() || name.equals("Normal") ? normal : CosmeticRegistry.tint(name, time);
	}
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
		if (!loadout.extra().isEmpty()) {
			poseStack.pushPose();
			getParentModel().body.translateAndRotate(poseStack);
			extra(poseStack, collector, light, state, loadout, time);
			poseStack.popPose();
		}
		if (loadout.hasMinimee()) {
			minimee(poseStack, collector, light, state, loadout);
		}
		if (!loadout.wings().isEmpty() && state.chestEquipment.getItem() != Items.ELYTRA) {
			poseStack.pushPose();
			getParentModel().body.translateAndRotate(poseStack);
			wings(poseStack, collector, light, state, loadout, time);
			poseStack.popPose();
		}
	}

	private void minimee(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l) {
		pose.pushPose();
		switch (l.minimeeMode()) {
			case "head" -> {
				getParentModel().head.translateAndRotate(pose);
				float top = -8.6F;
				if (!state.headEquipment.isEmpty()) {
					top = -9.8F;
				} else if (l.head().equals("tophat")) {
					top = -16.1F;
				} else {
					top = switch (l.head()) {
						case "crown" -> -10.3F;
						case "cowboyhat" -> -12.6F;
						case "partyhat" -> -15.4F;
						case "beanie" -> -12.6F;
						case "viking" -> -9.6F;
						case "flowercrown" -> -9.4F;
						case "headphones" -> -9.4F;
						default -> -8.6F;
					};
				}
				Minimees.head(pose, c, light, l.minimee(), top);
			}
			case "pocket" -> {
				getParentModel().rightLeg.translateAndRotate(pose);
				Minimees.pocket(pose, c, light, l.minimee());
			}
			default -> {
				getParentModel().body.translateAndRotate(pose);
				Minimees.neck(pose, c, light, l.minimee(), !state.chestEquipment.isEmpty() && state.chestEquipment.getItem() != Items.ELYTRA);
			}
		}
		pose.popPose();
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
					// the gold stays gold, the colour changes the stones (Normal: red rubies, white pearls)
					boolean normal = l.headColor().isEmpty() || l.headColor().equals("Normal");
					part(c, CosmeticModels.crown, pose, RenderTypes.entityCutout(CROWN), light, 0xFFFFFFFF);
					part(c, CosmeticModels.crownGems, pose, RenderTypes.entityCutout(CROWN_GEMS), light, normal ? 0xFFE0304E : tint);
					part(c, CosmeticModels.crownPearls, pose, RenderTypes.entityCutout(CROWN_GEMS), light, normal ? 0xFFFFFFFF : tint);
				}
			}
			case "tophat" -> {
				if (!helmet) {
					part(c, CosmeticModels.tophat, pose, RenderTypes.entityCutout(TOPHAT), light, 0xFFFFFFFF);
					int band = l.headColor().isEmpty() || l.headColor().equals("Normal") ? 0xFF8B5CF6 : tint;
					part(c, CosmeticModels.tophatBand, pose, RenderTypes.entityCutout(TOPHAT), light, band);
				}
			}
			case "horns" -> {
				// "Normal" = the red devil horns, every other colour uses the grey texture so the colour is exact
				boolean normal = l.headColor().isEmpty() || l.headColor().equals("Normal");
				part(c, CosmeticModels.horns, pose, RenderTypes.entityCutout(normal ? HORNS : HORNS_TINT), light, normal ? 0xFFFFFFFF : tint);
			}
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
			case "partyhat" -> {
				if (helmet) {
					return;
				}
				pose.pushPose();
				pose.translate(1.2F / 16F, -8F / 16F, 0);
				pose.rotate(Axis.ZP, 0.22F);
				pose.translate(0, 8F / 16F, 0);
				part(c, CosmeticModels.partyHat, pose, RenderTypes.entityCutout(PARTYHAT), light, color(l.headColor(), time, 0xFFEC4899));
				part(c, CosmeticModels.partyPom, pose, RenderTypes.entityCutout(PARTYHAT), light, 0xFFFFFFFF);
				pose.popPose();
			}
			case "cowboyhat" -> {
				if (helmet) {
					return;
				}
				part(c, CosmeticModels.cowboyHat, pose, RenderTypes.entityCutout(COWBOYHAT), light, 0xFFFFFFFF);
				part(c, CosmeticModels.cowboyBand, pose, RenderTypes.entityCutout(COWBOYHAT), light, color(l.headColor(), time, 0xFF4A2A12));
			}
			case "bunnyears" -> {
				if (helmet) {
					return;
				}
				int col = color(l.headColor(), time, 0xFFFFFFFF);
				for (int i = 0; i < 2; i++) {
					float side = i == 0 ? -1 : 1;
					float phase = (time + i * 2.3F) % 5.0F;
					float flop = phase < 0.5F ? Mth.sin(phase / 0.5F * Mth.PI) * 0.5F : 0;
					pose.pushPose();
					pose.translate(side * 1.9F / 16F, -7.9F / 16F, 0.6F / 16F);
					pose.rotate(Axis.ZP, side * 0.14F);
					pose.rotate(Axis.XP, -0.18F - (i == 1 ? flop : 0) - Math.min(1.0F, state.walkAnimationSpeed) * 0.25F);
					part(c, CosmeticModels.bunnyEar, pose, RenderTypes.entityCutout(BUNNYEARS), light, col);
					pose.popPose();
				}
			}
			case "headphones" -> {
				if (helmet) {
					return;
				}
				part(c, CosmeticModels.headphones, pose, RenderTypes.entityCutout(HEADPHONES), light, color(l.headColor(), time, 0xFF2E2E36));
			}
			case "sunglasses" -> part(c, CosmeticModels.sunglasses, pose, RenderTypes.entityCutout(SUNGLASSES), light, color(l.headColor(), time, 0xFF1C1C22));
			case "viking" -> {
				if (helmet) {
					return;
				}
				part(c, CosmeticModels.vikingHelmet, pose, RenderTypes.entityCutout(VIKING), light, 0xFFFFFFFF);
				part(c, CosmeticModels.vikingRim, pose, RenderTypes.entityCutout(VIKING), light, color(l.headColor(), time, 0xFFB8862B));
				for (int i = 0; i < 2; i++) {
					float side = i == 0 ? -1 : 1;
					pose.pushPose();
					pose.translate(side * 4.6F / 16F, -7.6F / 16F, 0);
					pose.rotate(Axis.ZP, side * 1.05F);
					part(c, CosmeticModels.vikingHorn, pose, RenderTypes.entityCutout(VIKING), light, 0xFFFFFFFF);
					pose.popPose();
				}
			}
			case "flowercrown" -> {
				if (helmet) {
					return;
				}
				part(c, CosmeticModels.flowerVine, pose, RenderTypes.entityCutout(FLOWERCROWN), light, 0xFFFFFFFF);
				int[] normal = {0xFFFF7FB2, 0xFFFFE066, 0xFF9AD0FF};
				for (int g = 0; g < 3; g++) {
					part(c, CosmeticModels.FLOWERS[g], pose, RenderTypes.entityCutout(FLOWERCROWN), light, color(l.headColor(), time, normal[g]));
				}
			}
			case "beanie" -> {
				if (helmet) {
					return;
				}
				int col = color(l.headColor(), time, 0xFFC62828);
				part(c, CosmeticModels.beanie, pose, RenderTypes.entityCutout(BEANIE), light, col);
				part(c, CosmeticModels.beaniePom, pose, RenderTypes.entityCutout(BEANIE), light, 0xFFFFFFFF);
			}
			default -> {
			}
		}
	}

	/** Body extras; the pose stack is in body space. */
	private void extra(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		boolean chest = !state.chestEquipment.isEmpty() && state.chestEquipment.getItem() != Items.ELYTRA;
		float armor = chest ? 1.0F : 0.0F;
		float walk = Math.min(1.0F, state.walkAnimationSpeed);
		switch (l.extra()) {
			case "backpack" -> {
				pose.pushPose();
				pose.translate(0, 0, armor / 16F);
				part(c, CosmeticModels.backpack, pose, RenderTypes.entityCutout(BACKPACK), light, color(l.extraColor(), time, 0xFF8D5A2B));
				pose.popPose();
				if (!chest) {
					part(c, CosmeticModels.backpackStraps, pose, RenderTypes.entityCutout(BACKPACK), light, 0xFFFFFFFF);
				}
			}
			case "cattail", "deviltail" -> {
				boolean devil = l.extra().equals("deviltail");
				int col = color(l.extraColor(), time, devil ? 0xFFC62828 : 0xFF3A3A44);
				pose.pushPose();
				// starts slightly downwards at the lower back and curls up (x rotation > 0 lifts the tail)
				pose.translate(0, 10.5F / 16F, (2.0F + armor * 0.5F) / 16F);
				pose.rotate(Axis.XP, -0.45F + walk * 0.25F);
				for (int i = 0; i < 5; i++) {
					pose.rotate(Axis.YP, Mth.sin(time * 2.5F - i * 0.7F) * (0.22F + walk * 0.1F));
					pose.rotate(Axis.XP, 0.3F);
					part(c, CosmeticModels.tailThin, pose, RenderTypes.entityCutout(TAILS), light, col);
					pose.translate(0, 0, 2.7F / 16F);
				}
				if (devil) {
					pose.rotate(Axis.ZP, Mth.PI / 4);
					part(c, CosmeticModels.tailArrow, pose, RenderTypes.entityCutout(TAILS), light, col);
				}
				pose.popPose();
			}
			case "foxtail" -> {
				int col = color(l.extraColor(), time, 0xFFE07020);
				pose.pushPose();
				// a fox tail hangs back and down and lifts a little at the end
				pose.translate(0, 10.0F / 16F, (2.0F + armor * 0.5F) / 16F);
				pose.rotate(Axis.XP, -0.6F + walk * 0.35F);
				for (int i = 0; i < 3; i++) {
					pose.rotate(Axis.YP, Mth.sin(time * 2.0F - i * 0.6F) * (0.18F + walk * 0.1F));
					pose.rotate(Axis.XP, 0.16F);
					float sc = 1.0F + i * 0.12F;
					pose.pushPose();
					pose.scale(sc, sc, 1.0F);
					part(c, CosmeticModels.tailFluffy, pose, RenderTypes.entityCutout(TAILS), light, col);
					pose.popPose();
					pose.translate(0, 0, 2.6F / 16F);
				}
				part(c, CosmeticModels.tailFluffyTip, pose, RenderTypes.entityCutout(TAILS), light, 0xFFFFFFFF);
				pose.popPose();
			}
			case "scarf" -> {
				int col = color(l.extraColor(), time, 0xFFD32F2F);
				pose.pushPose();
				if (chest) {
					pose.scale(1.12F, 1.0F, 1.35F);
				}
				part(c, CosmeticModels.scarf, pose, RenderTypes.entityCutout(SCARF), light, col);
				pose.popPose();
				pose.pushPose();
				pose.translate(-2.2F / 16F, 1.8F / 16F, -(2.9F + armor) / 16F);
				pose.rotate(Axis.XP, 0.12F + walk * 0.35F + Mth.sin(time * 3.0F) * 0.05F);
				part(c, CosmeticModels.scarfEnd, pose, RenderTypes.entityCutout(SCARF), light, col);
				pose.popPose();
			}
			default -> {
			}
		}
	}

	private void wings(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		boolean normal = l.wingsColor().isEmpty() || l.wingsColor().equals("Normal");
		// coloured wings use a grey version of the texture: tinting the coloured one gave muddy colours
		Identifier texture = tex("wings_" + l.wings() + (normal ? "" : "_tint"));
		boolean moving = state.walkAnimationSpeed > 0.15F;
		float speed = moving ? 5.5F : 2.2F;
		float amp = state.isCrouching ? 0.08F : moving ? 0.45F : 0.25F;
		float flap = (Mth.sin(time * speed) + 1.0F) / 2.0F * amp;
		int tint = normal ? 0xFFFFFFFF : CosmeticRegistry.tint(l.wingsColor(), time);
		RenderType type = l.wings().equals("phoenix") ? RenderTypes.entityTranslucentEmissive(texture) : RenderTypes.entityTranslucent(texture);
		if (l.wings().equals("phoenix")) {
			light = LightCoordsUtil.FULL_BRIGHT;
		}
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
