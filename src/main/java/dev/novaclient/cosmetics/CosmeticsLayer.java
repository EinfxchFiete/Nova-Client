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
	private static final Identifier WITCHHAT = tex("witchhat");
	private static final Identifier SANTAHAT = tex("santahat");
	private static final Identifier CHEFHAT = tex("chefhat");
	private static final Identifier FOXEARS = tex("foxears");
	private static final Identifier UNICORN = tex("unicorn");
	private static final Identifier PROPELLER = tex("propeller");
	private static final Identifier JETPACK = tex("jetpack");
	private static final Identifier GUITAR = tex("guitar");
	private static final Identifier BACKSWORD = tex("backsword");
	private static final Identifier MUSHROOM = tex("mushroom");
	private static final Identifier PIRATEHAT = tex("piratehat");
	private static final Identifier ANTENNAE = tex("antennae");
	private static final Identifier GRADCAP = tex("gradcap");
	private static final Identifier MOHAWK = tex("mohawk");
	private static final Identifier VISOR = tex("visor");
	private static final Identifier DUCK = tex("duck");
	private static final Identifier FACEMASK = tex("facemask");
	private static final Identifier SHIELD = tex("shield");
	private static final Identifier QUIVER = tex("quiver");
	private static final Identifier BALLOON = tex("balloon");

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
		float time = state.ageInTicks / 20.0F;
		boolean chestArmor = !state.chestEquipment.isEmpty() && state.chestEquipment.getItem() != Items.ELYTRA;
		pose.pushPose();
		switch (l.minimeeMode()) {
			case "head" -> {
				getParentModel().head.translateAndRotate(pose);
				float top = -8.6F;
				if (!state.headEquipment.isEmpty()) {
					top = -9.8F;
				} else {
					top = switch (l.head()) {
						case "tophat" -> -16.1F;
						case "crown" -> -10.3F;
						case "cowboyhat" -> -12.6F;
						case "partyhat" -> -15.4F;
						case "beanie" -> -12.6F;
						case "viking" -> -9.6F;
						case "flowercrown" -> -9.4F;
						case "headphones" -> -9.4F;
						case "witchhat" -> -14.6F;
						case "santahat" -> -15.2F;
						case "chefhat" -> -15.4F;
						case "propeller" -> -12.4F;
						case "mushroom" -> -12.3F;
						case "piratehat" -> -11.6F;
						case "gradcap" -> -10.7F;
						case "mohawk" -> -11.6F;
						case "duck" -> -13.9F;
						default -> -8.6F;
					};
				}
				Minimees.head(pose, c, light, state, l, time, top);
			}
			case "pocket" -> {
				getParentModel().rightLeg.translateAndRotate(pose);
				Minimees.pocket(pose, c, light, state, l, time);
			}
			case "hand" -> {
				getParentModel().rightArm.translateAndRotate(pose);
				Minimees.hand(pose, c, light, state, l, time);
			}
			case "shoulder" -> {
				getParentModel().body.translateAndRotate(pose);
				Minimees.shoulder(pose, c, light, state, l, time, chestArmor);
			}
			case "back" -> {
				getParentModel().body.translateAndRotate(pose);
				Minimees.back(pose, c, light, state, l, time, chestArmor);
			}
			case "float" -> Minimees.floating(pose, c, light, state, l, time);
			default -> {
				getParentModel().body.translateAndRotate(pose);
				Minimees.neck(pose, c, light, state, l, time, chestArmor);
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
			case "witchhat" -> {
				if (helmet) {
					return;
				}
				RenderType type = RenderTypes.entityCutout(WITCHHAT);
				int col = color(l.headColor(), time, 0xFF3E2A5C);
				part(c, CosmeticModels.witchHat, pose, type, light, col);
				part(c, CosmeticModels.witchBand, pose, type, light, 0xFFFF9800);
				// the bent top sways a little when you walk
				float sway = Mth.sin(time * 2.2F) * 0.06F + Math.min(1.0F, state.walkAnimationSpeed) * 0.12F;
				pose.pushPose();
				pose.translate(0, -14.4F / 16F, 1.0F / 16F);
				pose.rotate(Axis.XP, -0.35F - sway);
				part(c, CosmeticModels.witchTop, pose, type, light, col);
				pose.translate(0, -2.6F / 16F, 0);
				pose.rotate(Axis.XP, -0.55F - sway);
				part(c, CosmeticModels.witchTip, pose, type, light, col);
				pose.popPose();
			}
			case "santahat" -> {
				if (helmet) {
					return;
				}
				RenderType type = RenderTypes.entityCutout(SANTAHAT);
				int col = color(l.headColor(), time, 0xFFD32F2F);
				part(c, CosmeticModels.santaFur, pose, type, light, 0xFFFFFFFF);
				part(c, CosmeticModels.santaBody, pose, type, light, col);
				float swing = Mth.sin(time * 3.0F) * 0.08F + Math.min(1.0F, state.walkAnimationSpeed) * 0.25F;
				pose.pushPose();
				pose.translate(0, -15.0F / 16F, 0.4F / 16F);
				pose.rotate(Axis.XP, -0.7F - swing);
				pose.rotate(Axis.ZP, 0.45F);
				part(c, CosmeticModels.santaDroop1, pose, type, light, col);
				pose.translate(0, -2.6F / 16F, 0);
				pose.rotate(Axis.XP, -0.6F - swing);
				pose.rotate(Axis.ZP, 0.3F);
				part(c, CosmeticModels.santaDroop2, pose, type, light, col);
				pose.translate(0, -2.4F / 16F, 0);
				part(c, CosmeticModels.santaPom, pose, type, light, 0xFFFFFFFF);
				pose.popPose();
			}
			case "chefhat" -> {
				if (!helmet) {
					part(c, CosmeticModels.chefHat, pose, RenderTypes.entityCutout(CHEFHAT), light, color(l.headColor(), time, 0xFFFFFFFF));
				}
			}
			case "foxears" -> {
				if (helmet) {
					return;
				}
				int col = color(l.headColor(), time, 0xFFE8742A);
				for (int i = 0; i < 2; i++) {
					float side = i == 0 ? -1 : 1;
					float phase = (time + i * 1.3F) % 3.5F;
					float twitch = phase < 0.25F ? Mth.sin(phase / 0.25F * Mth.PI) * 0.4F : 0;
					pose.pushPose();
					pose.translate(side * 2.7F / 16F, -7.9F / 16F, -0.4F / 16F);
					pose.rotate(Axis.ZP, side * (0.22F + twitch));
					pose.rotate(Axis.XP, -0.1F);
					part(c, CosmeticModels.foxEar, pose, RenderTypes.entityCutout(FOXEARS), light, col);
					part(c, CosmeticModels.foxEarInner, pose, RenderTypes.entityCutout(FOXEARS), light, 0xFFFFFFFF);
					pose.popPose();
				}
			}
			case "unicorn" -> {
				pose.pushPose();
				pose.translate(0, -7.2F / 16F, -3.4F / 16F);
				pose.rotate(Axis.XP, 0.5F);
				pose.rotate(Axis.YP, time * 1.2F);
				int col = color(l.headColor(), time, 0xFFFFE9A6);
				part(c, CosmeticModels.unicornHorn, pose, RenderTypes.entityCutout(UNICORN), light, col);
				pose.popPose();
			}
			case "mushroom" -> {
				if (helmet) {
					return;
				}
				RenderType type = RenderTypes.entityCutout(MUSHROOM);
				part(c, CosmeticModels.mushroomRing, pose, type, light, 0xFFFFFFFF);
				part(c, CosmeticModels.mushroomCap, pose, type, light, color(l.headColor(), time, 0xFFD32F2F));
				part(c, CosmeticModels.mushroomSpots, pose, type, light, 0xFFFFFFFF);
			}
			case "piratehat" -> {
				if (helmet) {
					return;
				}
				RenderType type = RenderTypes.entityCutout(PIRATEHAT);
				part(c, CosmeticModels.pirateHat, pose, type, light, 0xFFFFFFFF);
				part(c, CosmeticModels.pirateBand, pose, type, light, color(l.headColor(), time, 0xFFFFC107));
			}
			case "antennae" -> {
				RenderType type = RenderTypes.entityCutout(ANTENNAE);
				int col = color(l.headColor(), time, 0xFF76FF03);
				float walk = Math.min(1.0F, state.walkAnimationSpeed);
				for (int i = 0; i < 2; i++) {
					float side = i == 0 ? -1 : 1;
					// springy bobbing, more when you walk
					float bob = Mth.sin(time * 5.0F + i * 1.9F) * (0.07F + walk * 0.12F);
					pose.pushPose();
					pose.translate(side * 2.2F / 16F, -(helmet ? 9.6F : 8.0F) / 16F, -0.5F / 16F);
					pose.rotate(Axis.ZP, side * 0.32F + bob);
					pose.rotate(Axis.XP, Mth.sin(time * 3.7F + i) * 0.08F);
					part(c, CosmeticModels.antennaStalk, pose, type, light, 0xFFFFFFFF);
					pose.translate(0, -4.6F / 16F, 0);
					part(c, CosmeticModels.antennaBall, pose, type, LightCoordsUtil.FULL_BRIGHT, col);
					pose.popPose();
				}
			}
			case "gradcap" -> {
				if (helmet) {
					return;
				}
				RenderType type = RenderTypes.entityCutout(GRADCAP);
				int col = color(l.headColor(), time, 0xFFFFC107);
				part(c, CosmeticModels.gradCap, pose, type, light, 0xFFFFFFFF);
				part(c, CosmeticModels.gradButton, pose, type, light, col);
				// the tassel hangs over the right edge and swings while you walk
				float swing = Mth.sin(time * 4.0F) * (0.06F + Math.min(1.0F, state.walkAnimationSpeed) * 0.3F);
				pose.pushPose();
				pose.translate(5.3F / 16F, -10.5F / 16F, 1.5F / 16F);
				pose.rotate(Axis.XP, swing);
				pose.rotate(Axis.ZP, swing * 0.4F);
				part(c, CosmeticModels.gradTassel, pose, type, light, col);
				pose.popPose();
			}
			case "mohawk" -> {
				if (helmet) {
					return;
				}
				part(c, CosmeticModels.mohawk, pose, RenderTypes.entityCutout(MOHAWK), light, color(l.headColor(), time, 0xFFEC407A));
			}
			case "visor" -> {
				part(c, CosmeticModels.visorTemples, pose, RenderTypes.entityCutout(VISOR), light, 0xFFFFFFFF);
				part(c, CosmeticModels.visorGlass, pose, RenderTypes.entityCutout(VISOR), LightCoordsUtil.FULL_BRIGHT, color(l.headColor(), time, 0xFF18FFFF));
			}
			case "duck" -> {
				if (helmet) {
					return;
				}
				RenderType type = RenderTypes.entityCutout(DUCK);
				// wobbles a little, more when you walk
				float wobble = Mth.sin(time * 6.0F) * (0.04F + Math.min(1.0F, state.walkAnimationSpeed) * 0.14F);
				pose.pushPose();
				pose.translate(0, -8.0F / 16F, 0);
				pose.rotate(Axis.ZP, wobble);
				part(c, CosmeticModels.duckBody, pose, type, light, color(l.headColor(), time, 0xFFFFE04A));
				part(c, CosmeticModels.duckBeak, pose, type, light, 0xFFFFFFFF);
				pose.popPose();
			}
			case "facemask" -> {
				RenderType type = RenderTypes.entityCutout(FACEMASK);
				part(c, CosmeticModels.faceMaskStraps, pose, type, light, 0xFFFFFFFF);
				part(c, CosmeticModels.faceMask, pose, type, light, color(l.headColor(), time, 0xFF90CAF9));
			}
			case "propeller" -> {
				if (helmet) {
					return;
				}
				RenderType type = RenderTypes.entityCutout(PROPELLER);
				part(c, CosmeticModels.propellerCap, pose, type, light, 0xFFFFFFFF);
				// spins faster when you run
				float spin = time * (9.0F + Math.min(1.0F, state.walkAnimationSpeed) * 14.0F);
				pose.pushPose();
				pose.translate(0, -12.1F / 16F, 0);
				pose.rotate(Axis.YP, spin);
				part(c, CosmeticModels.propellerBlade, pose, type, light, color(l.headColor(), time, 0xFFFFD54F));
				pose.popPose();
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
			case "jetpack" -> {
				pose.pushPose();
				pose.translate(0, 0, armor / 16F);
				part(c, CosmeticModels.jetpack, pose, RenderTypes.entityCutout(JETPACK), light, 0xFFFFFFFF);
				part(c, CosmeticModels.jetpackStripes, pose, RenderTypes.entityCutout(JETPACK), light, color(l.extraColor(), time, 0xFFE53935));
				pose.popPose();
			}
			case "guitar" -> {
				pose.pushPose();
				// diagonal on the back: body low on the left, neck over the right shoulder; the strings face outwards
				pose.translate(1.2F / 16F, 7.4F / 16F, (2.15F + armor) / 16F);
				pose.rotate(Axis.ZP, -0.62F);
				part(c, CosmeticModels.guitarBody, pose, RenderTypes.entityCutout(GUITAR), light, color(l.extraColor(), time, 0xFFC27A3A));
				part(c, CosmeticModels.guitarNeck, pose, RenderTypes.entityCutout(GUITAR), light, 0xFFFFFFFF);
				pose.popPose();
			}
			case "shield" -> {
				pose.pushPose();
				pose.translate(0, 0, armor / 16F);
				part(c, CosmeticModels.shieldPlate, pose, RenderTypes.entityCutout(SHIELD), light, color(l.extraColor(), time, 0xFF1E88E5));
				part(c, CosmeticModels.shieldBoss, pose, RenderTypes.entityCutout(SHIELD), light, 0xFFFFFFFF);
				pose.popPose();
			}
			case "quiver" -> {
				pose.pushPose();
				// diagonal on the back, opening over the right shoulder
				pose.translate(-0.6F / 16F, 4.6F / 16F, (2.1F + armor) / 16F);
				pose.rotate(Axis.ZP, -0.5F);
				part(c, CosmeticModels.quiverTube, pose, RenderTypes.entityCutout(QUIVER), light, color(l.extraColor(), time, 0xFF8D5A2B));
				part(c, CosmeticModels.quiverArrows, pose, RenderTypes.entityCutout(QUIVER), light, 0xFFFFFFFF);
				pose.popPose();
			}
			case "balloon" -> {
				pose.pushPose();
				// tied to the left shoulder; sways in the wind and trails behind while you walk
				pose.translate(4.5F / 16F, 0.2F / 16F, 1.0F / 16F);
				pose.rotate(Axis.XP, -walk * 0.5F + Mth.sin(time * 1.7F) * 0.06F);
				pose.rotate(Axis.ZP, 0.12F + Mth.sin(time * 1.3F) * 0.09F);
				part(c, CosmeticModels.balloonString, pose, RenderTypes.entityCutout(BALLOON), light, 0xFFFFFFFF);
				part(c, CosmeticModels.balloonBody, pose, RenderTypes.entityCutout(BALLOON), light, color(l.extraColor(), time, 0xFFE53935));
				pose.popPose();
			}
			case "backsword" -> {
				pose.pushPose();
				// hilt over the right shoulder, blade diagonally down to the left hip
				pose.translate(-2.6F / 16F, 0.4F / 16F, (2.7F + armor) / 16F);
				pose.rotate(Axis.ZP, -0.62F);
				part(c, CosmeticModels.swordBlade, pose, RenderTypes.entityCutout(BACKSWORD), light, color(l.extraColor(), time, 0xFF7FE8E4));
				part(c, CosmeticModels.swordHilt, pose, RenderTypes.entityCutout(BACKSWORD), light, 0xFFFFFFFF);
				pose.popPose();
			}
			default -> {
			}
		}
	}

	private void wings(PoseStack pose, SubmitNodeCollector c, int light, AvatarRenderState state, Loadout l, float time) {
		boolean normal = l.wingsColor().isEmpty() || l.wingsColor().equals("Normal");
		boolean phoenix = l.wings().equals("phoenix");
		// coloured wings use a grey version of the texture: tinting the coloured one gave muddy colours.
		// Phoenix wings are animated (4 flame frames, 8 per second).
		String name = "wings_" + l.wings() + (normal ? "" : "_tint");
		if (phoenix) {
			name += "_f" + (int) (time * 8.0F) % 4;
		}
		Identifier texture = tex(name);
		boolean moving = state.walkAnimationSpeed > 0.15F;
		float speed = moving ? 5.5F : 2.2F;
		float amp = state.isCrouching ? 0.08F : moving ? 0.45F : 0.25F;
		float flap = (Mth.sin(time * speed) + 1.0F) / 2.0F * amp;
		int tint = normal ? 0xFFFFFFFF : CosmeticRegistry.tint(l.wingsColor(), time);
		// all wings use the double sided translucent type: the left wing is mirrored (scale -1), and the emissive type
		// culls back faces, which made one phoenix wing vanish from most angles. Phoenix wings glow via full light.
		RenderType type = RenderTypes.entityTranslucent(texture);
		if (phoenix) {
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
