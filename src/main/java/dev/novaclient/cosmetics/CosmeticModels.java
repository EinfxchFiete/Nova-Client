package dev.novaclient.cosmetics;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;

/**
 * The 3D models of all cosmetics. Coordinates are in model pixels relative to the head (pivot at the neck,
 * head cube from -4,-8,-4 to 4,0,4; the face points to -Z) or the body (pivot at the neck, back at z = +2).
 * Moving pieces (bandana tails, ears, wings) are separate parts so they can be animated with the pose stack.
 */
public final class CosmeticModels {
	private static final Set<Direction> SIDES = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

	public static ModelPart bandana;
	public static ModelPart bandanaTail;
	public static ModelPart crown;
	public static ModelPart crownGems;
	public static ModelPart crownPearls;
	public static ModelPart chainLink;
	public static ModelPart tophat;
	public static ModelPart tophatBand;
	public static ModelPart horns;
	public static ModelPart catEar;
	public static ModelPart halo;
	public static ModelPart wing;
	// 1.6.0
	public static ModelPart partyHat;
	public static ModelPart partyPom;
	public static ModelPart cowboyHat;
	public static ModelPart cowboyBand;
	public static ModelPart bunnyEar;
	public static ModelPart headphones;
	public static ModelPart sunglasses;
	public static ModelPart vikingHelmet;
	public static ModelPart vikingRim;
	public static ModelPart vikingHorn;
	public static ModelPart flowerVine;
	public static final ModelPart[] FLOWERS = new ModelPart[3];
	public static ModelPart beanie;
	public static ModelPart beaniePom;
	public static ModelPart backpack;
	public static ModelPart backpackStraps;
	public static ModelPart tailThin;
	public static ModelPart tailFluffy;
	public static ModelPart tailFluffyTip;
	public static ModelPart tailArrow;
	public static ModelPart scarf;
	public static ModelPart scarfEnd;
	// 1.8.0
	public static ModelPart witchHat;
	public static ModelPart witchTop;
	public static ModelPart witchTip;
	public static ModelPart witchBand;
	public static ModelPart santaFur;
	public static ModelPart santaBody;
	public static ModelPart santaDroop1;
	public static ModelPart santaDroop2;
	public static ModelPart santaPom;
	public static ModelPart chefHat;
	public static ModelPart foxEar;
	public static ModelPart foxEarInner;
	public static ModelPart unicornHorn;
	public static ModelPart propellerCap;
	public static ModelPart propellerBlade;
	public static ModelPart jetpack;
	public static ModelPart jetpackStripes;
	public static ModelPart guitarBody;
	public static ModelPart guitarNeck;
	public static ModelPart swordBlade;
	public static ModelPart swordHilt;
	// 1.8.1
	public static ModelPart mushroomCap;
	public static ModelPart mushroomSpots;
	public static ModelPart mushroomRing;
	public static ModelPart pirateHat;
	public static ModelPart pirateBand;
	public static ModelPart antennaStalk;
	public static ModelPart antennaBall;
	// 1.8.5
	public static ModelPart gradCap;
	public static ModelPart gradButton;
	public static ModelPart gradTassel;
	public static ModelPart mohawk;
	public static ModelPart visorGlass;
	public static ModelPart visorTemples;
	public static ModelPart duckBody;
	public static ModelPart duckBeak;
	public static ModelPart faceMask;
	public static ModelPart faceMaskStraps;
	public static ModelPart shieldPlate;
	public static ModelPart shieldBoss;
	public static ModelPart quiverTube;
	public static ModelPart quiverArrows;
	public static ModelPart balloonString;
	public static ModelPart balloonBody;

	private static boolean built;

	private CosmeticModels() {
	}

	private static ModelPart bake(MeshDefinition mesh, int w, int h) {
		return LayerDefinition.create(mesh, w, h).bakeRoot();
	}

	/** Builds all models once (must run after the game initialised its model system). */
	public static void ensureBuilt() {
		if (built) {
			return;
		}
		built = true;

		MeshDefinition m = new MeshDefinition();
		PartDefinition r = m.getRoot();
		r.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -7, -4, 8, 2, 8, new CubeDeformation(0.35F)), PartPose.ZERO);
		r.addOrReplaceChild("knot", CubeListBuilder.create().texOffs(0, 12).addBox(-1.5F, -7.2F, 4.2F, 3, 2, 2), PartPose.ZERO);
		bandana = bake(m, 64, 32);

		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 18).addBox(-1, 0, 0, 2, 5, 1), PartPose.ZERO);
		bandanaTail = bake(m, 64, 32);

		// crown: gold ring around the top of the head (wider than the hat layer so it sits ON the head, not in it)
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 0).addBox(-5.15F, -10.15F, -5.15F, 10.3F, 2.3F, 10.3F, SIDES), PartPose.ZERO);
		float[][] spikes = {{-5, -5}, {4, -5}, {-5, 4}, {4, 4}, {-0.5F, -5}, {-0.5F, 4}, {-5, -0.5F}, {4, -0.5F}};
		for (int i = 0; i < spikes.length; i++) {
			r.addOrReplaceChild("spike" + i, CubeListBuilder.create().texOffs(0, 14).addBox(spikes[i][0], -12, spikes[i][1], 1, 2, 1, new CubeDeformation(0.1F)), PartPose.ZERO);
		}
		crown = bake(m, 64, 32);
		// the stones of the crown: separate part so they can be coloured
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -9.5F, -5.75F, 2, 1, 1), PartPose.ZERO);
		r.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -9.5F, 4.75F, 2, 1, 1), PartPose.ZERO);
		r.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 4).addBox(-5.75F, -9.5F, -1, 1, 1, 2), PartPose.ZERO);
		r.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 4).addBox(4.75F, -9.5F, -1, 1, 1, 2), PartPose.ZERO);
		crownGems = bake(m, 32, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		for (int i = 0; i < spikes.length; i++) {
			r.addOrReplaceChild("pearl" + i, CubeListBuilder.create().texOffs(0, 8).addBox(spikes[i][0], -12.9F, spikes[i][1], 1, 1, 1, new CubeDeformation(0.05F)), PartPose.ZERO);
		}
		crownPearls = bake(m, 32, 32);

		// one chain link segment (1 px thick, 1 px long along +y), stretched with the pose stack
		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("link", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 0, -0.5F, 1, 1, 1), PartPose.ZERO);
		chainLink = bake(m, 16, 16);

		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, -9, -5.5F, 11, 1, 11), PartPose.ZERO);
		r.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 16).addBox(-3.5F, -16, -3.5F, 7, 7, 7), PartPose.ZERO);
		tophat = bake(m, 64, 64);
		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 40).addBox(-3.5F, -11, -3.5F, 7, 2, 7, new CubeDeformation(0.12F)), PartPose.ZERO);
		tophatBand = bake(m, 64, 64);

		m = new MeshDefinition();
		r = m.getRoot();
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "left" : "right";
			PartDefinition base = r.addOrReplaceChild(n + "_base", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -3, -1, 2, 3, 2),
				PartPose.offsetAndRotation(side * 2.6F, -7.6F, -1.5F, -0.25F, 0, side * 0.45F));
			base.addOrReplaceChild(n + "_tip", CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, -3, -0.5F, 1, 3, 1),
				PartPose.offsetAndRotation(0, -2.6F, 0, -0.35F, 0, side * 0.35F));
		}
		horns = bake(m, 32, 32);

		m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("ear", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3, -0.5F, 3, 3, 1), PartPose.ZERO);
		catEar = bake(m, 32, 32);

		m = new MeshDefinition();
		r = m.getRoot();
		int segments = 16;
		for (int i = 0; i < segments; i++) {
			double a = i * Math.PI * 2 / segments;
			r.addOrReplaceChild("seg" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.9F, -0.35F, -0.45F, 1.8F, 0.7F, 0.9F),
				PartPose.offsetAndRotation((float) (Math.cos(a) * 4.6), 0, (float) (Math.sin(a) * 4.6), 0, (float) -a, 0));
		}
		halo = bake(m, 16, 16);

		m = new MeshDefinition();
		// a flat 16x16 plane: x = 0 is the spine, it grows to +x; y from -10 (top) to +6
		m.getRoot().addOrReplaceChild("plane", CubeListBuilder.create().texOffs(0, 0).addBox(0, -10, 0, 16, 16, 0), PartPose.ZERO);
		wing = bake(m, 64, 32);

		buildV16();
		buildV18();
		buildV181();
		buildV185();
	}

	/** Models of the 1.8.5 cosmetics (head space for hats, body space for extras). */
	private static void buildV185() {
		// graduation cap: skull cap + flat board; the tassel hangs from the middle button over the edge
		MeshDefinition m = new MeshDefinition();
		PartDefinition r = m.getRoot();
		r.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -9.6F, -4, 8, 2, 8, new CubeDeformation(0.3F)), PartPose.ZERO);
		r.addOrReplaceChild("board", CubeListBuilder.create().texOffs(0, 12).addBox(-5.5F, -10.9F, -5.5F, 11, 1, 11, new CubeDeformation(0, -0.3F, 0)), PartPose.ZERO);
		gradCap = bake(m, 64, 64);
		gradButton = single(CubeListBuilder.create().texOffs(0, 26).addBox(-0.5F, -11.2F, -0.5F, 1, 1, 1, new CubeDeformation(0, -0.25F, 0)), 64, 64);
		gradTassel = single(CubeListBuilder.create().texOffs(8, 26).addBox(-0.5F, 0, -0.5F, 1, 4, 1, new CubeDeformation(-0.15F, 0, -0.15F)), 64, 64);

		mohawk = single(CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -11.6F, -4.5F, 1, 4, 9), 32, 32);

		visorGlass = single(CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -5.6F, -5.0F, 9, 2, 1, new CubeDeformation(0.05F)), 64, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("tl", CubeListBuilder.create().texOffs(0, 4).addBox(-4.75F, -5.1F, -4.3F, 1, 1, 5), PartPose.ZERO);
		r.addOrReplaceChild("tr", CubeListBuilder.create().texOffs(0, 4).addBox(3.75F, -5.1F, -4.3F, 1, 1, 5), PartPose.ZERO);
		visorTemples = bake(m, 64, 32);

		// rubber duck sitting on the head (origin = where it sits, looking forward)
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -3, -2.5F, 4, 3, 5), PartPose.ZERO);
		r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -5.5F, -3.5F, 3, 3, 3), PartPose.ZERO);
		r.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(8, 14).addBox(-1, -3.6F, 2.2F, 2, 1, 1), PartPose.ZERO);
		duckBody = bake(m, 32, 32);
		duckBeak = single(CubeListBuilder.create().texOffs(0, 14).addBox(-1, -3.9F, -4.5F, 2, 1, 1, new CubeDeformation(0, -0.15F, 0)), 32, 32);

		faceMask = single(CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3.6F, -4.9F, 8, 3, 1, new CubeDeformation(0.25F, 0, -0.1F)), 64, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("sl", CubeListBuilder.create().texOffs(0, 4).addBox(-4.8F, -2.6F, -4.2F, 1, 1, 5, new CubeDeformation(0, -0.25F, 0)), PartPose.ZERO);
		r.addOrReplaceChild("sr", CubeListBuilder.create().texOffs(0, 4).addBox(3.8F, -2.6F, -4.2F, 1, 1, 5, new CubeDeformation(0, -0.25F, 0)), PartPose.ZERO);
		faceMaskStraps = bake(m, 64, 32);

		// extras (body space: neck at y 0, back at z +2)
		shieldPlate = single(CubeListBuilder.create().texOffs(0, 0).addBox(-4, 1, 2.2F, 8, 9, 1), 64, 32);
		shieldBoss = single(CubeListBuilder.create().texOffs(20, 0).addBox(-1, 4.5F, 2.9F, 2, 2, 1, new CubeDeformation(0, 0, -0.2F)), 64, 32);

		// quiver: origin = middle of the tube's back side, opening at the top (-y)
		quiverTube = single(CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -4.5F, 0, 3, 9, 2), 32, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		float[][] arrows = {{-1.3F, -7.2F, 0.3F}, {0.3F, -7.8F, 0.6F}, {-0.5F, -6.6F, 0.9F}};
		for (int i = 0; i < arrows.length; i++) {
			r.addOrReplaceChild("a" + i, CubeListBuilder.create().texOffs(12, 0).addBox(arrows[i][0], arrows[i][1], arrows[i][2], 1, 3, 1,
				new CubeDeformation(-0.15F, 0, -0.15F)), PartPose.ZERO);
		}
		quiverArrows = bake(m, 32, 32);

		// balloon: origin = where the string is held, everything grows up (-y)
		balloonString = single(CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -12, -0.5F, 1, 12, 1, new CubeDeformation(-0.38F, 0, -0.38F)), 32, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("knot", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -12.6F, -0.5F, 1, 1, 1), PartPose.ZERO);
		r.addOrReplaceChild("balloon", CubeListBuilder.create().texOffs(0, 14).addBox(-2.5F, -18.4F, -2.5F, 5, 6, 5), PartPose.ZERO);
		balloonBody = bake(m, 32, 32);
	}

	/** Models of the 1.8.1 cosmetics (head space). */
	private static void buildV181() {
		MeshDefinition m = new MeshDefinition();
		PartDefinition r = m.getRoot();
		r.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, -10, -5.5F, 11, 2, 11), PartPose.ZERO);
		r.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 14).addBox(-4, -12, -4, 8, 2, 8), PartPose.ZERO);
		mushroomCap = bake(m, 64, 64);
		m = new MeshDefinition();
		r = m.getRoot();
		float[][] spots = {{-3, -12.3F, -1}, {1, -12.3F, 1.2F}, {-0.8F, -12.3F, -3.2F}, {-5.4F, -10.3F, 0.5F}, {3.5F, -10.3F, -2.5F}, {0.5F, -10.3F, 3.6F}};
		for (int i = 0; i < spots.length; i++) {
			r.addOrReplaceChild("s" + i, CubeListBuilder.create().texOffs(44, 0).addBox(spots[i][0], spots[i][1], spots[i][2], 2, 1, 2,
				new CubeDeformation(0, -0.35F, 0)), PartPose.ZERO);
		}
		mushroomSpots = bake(m, 64, 64);
		mushroomRing = single(CubeListBuilder.create().texOffs(0, 26).addBox(-4.5F, -8.4F, -4.5F, 9, 1, 9, new CubeDeformation(0.2F)), 64, 64);

		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -8.6F, -6, 12, 1, 12), PartPose.ZERO);
		r.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 13).addBox(-4.5F, -11.6F, -4.5F, 9, 3, 9), PartPose.ZERO);
		pirateHat = bake(m, 64, 64);
		pirateBand = single(CubeListBuilder.create().texOffs(0, 27).addBox(-4.5F, -9.6F, -4.5F, 9, 1, 9, new CubeDeformation(0.15F)), 64, 64);

		antennaStalk = single(CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -5, -0.5F, 1, 5, 1, new CubeDeformation(-0.2F)), 32, 16);
		antennaBall = single(CubeListBuilder.create().texOffs(8, 0).addBox(-1, -2, -1, 2, 2, 2), 32, 16);
	}

	/** Models of the 1.8.0 cosmetics (head space for hats, body space for extras). */
	private static void buildV18() {
		// witch hat: brim + cone; the top two pieces bend backwards (separate parts, animated in the layer)
		MeshDefinition m = new MeshDefinition();
		PartDefinition r = m.getRoot();
		r.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -8.6F, -6, 12, 1, 12), PartPose.ZERO);
		r.addOrReplaceChild("l1", CubeListBuilder.create().texOffs(0, 13).addBox(-4, -11.6F, -4, 8, 3, 8), PartPose.ZERO);
		r.addOrReplaceChild("l2", CubeListBuilder.create().texOffs(32, 13).addBox(-3, -14.6F, -2.5F, 6, 3, 6), PartPose.ZERO);
		witchHat = bake(m, 64, 64);
		witchTop = single(CubeListBuilder.create().texOffs(0, 25).addBox(-2, -3, -2, 4, 3, 4), 64, 64);
		witchTip = single(CubeListBuilder.create().texOffs(24, 25).addBox(-1, -3, -1, 2, 3, 2), 64, 64);
		witchBand = single(CubeListBuilder.create().texOffs(0, 40).addBox(-4, -9.8F, -4, 8, 1, 8, new CubeDeformation(0.15F)), 64, 64);

		// santa hat: white fur rim, red body, a drooping tip with a pompom
		santaFur = single(CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -9.4F, -4.5F, 9, 2, 9, new CubeDeformation(0.2F)), 64, 64);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("b1", CubeListBuilder.create().texOffs(0, 12).addBox(-4, -12.4F, -4, 8, 3, 8), PartPose.ZERO);
		r.addOrReplaceChild("b2", CubeListBuilder.create().texOffs(32, 12).addBox(-3, -15.2F, -3, 6, 3, 6), PartPose.ZERO);
		santaBody = bake(m, 64, 64);
		santaDroop1 = single(CubeListBuilder.create().texOffs(0, 24).addBox(-2, -3, -2, 4, 3, 4), 64, 64);
		santaDroop2 = single(CubeListBuilder.create().texOffs(20, 24).addBox(-1.5F, -3, -1.5F, 3, 3, 3), 64, 64);
		santaPom = single(CubeListBuilder.create().texOffs(40, 24).addBox(-1.5F, -3, -1.5F, 3, 3, 3, new CubeDeformation(0.1F)), 64, 64);

		// chef hat: band and a big puff
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -9.6F, -4, 8, 2, 8, new CubeDeformation(0.3F)), PartPose.ZERO);
		r.addOrReplaceChild("puff", CubeListBuilder.create().texOffs(0, 12).addBox(-5, -14.6F, -5, 10, 5, 10), PartPose.ZERO);
		r.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 30).addBox(-4, -15.4F, -4, 8, 1, 8), PartPose.ZERO);
		chefHat = bake(m, 64, 64);

		// fox ear (pointed), the white inside is its own part so it is never tinted
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("ear", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -4, -0.5F, 3, 4, 1), PartPose.ZERO);
		r.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, -5, -0.5F, 1, 1, 1), PartPose.ZERO);
		foxEar = bake(m, 32, 32);
		foxEarInner = single(CubeListBuilder.create().texOffs(8, 8).addBox(-0.5F, -3.2F, -1.0F, 1, 2, 1, new CubeDeformation(0, 0, -0.35F)), 32, 32);

		// unicorn horn: a spiral of thinner and thinner pieces (base at the origin, growing up)
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("s1", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -2, -1, 2, 2, 2), PartPose.ZERO);
		r.addOrReplaceChild("s2", CubeListBuilder.create().texOffs(8, 0).addBox(-1, -4, -1, 2, 2, 2, new CubeDeformation(-0.25F)),
			PartPose.rotation(0, 0.6F, 0));
		r.addOrReplaceChild("s3", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -6, -0.5F, 1, 2, 1), PartPose.rotation(0, 1.2F, 0));
		r.addOrReplaceChild("s4", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -7, -0.5F, 1, 1, 1, new CubeDeformation(-0.15F)),
			PartPose.rotation(0, 1.8F, 0));
		unicornHorn = bake(m, 32, 16);

		// propeller cap: colourful cap with a visor, the blade spins
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -9.6F, -4, 8, 2, 8, new CubeDeformation(0.3F)), PartPose.ZERO);
		r.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(0, 12).addBox(-4, -8.0F, -7.2F, 8, 1, 3), PartPose.ZERO);
		r.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(0, 18).addBox(-0.5F, -11.8F, -0.5F, 1, 2, 1), PartPose.ZERO);
		propellerCap = bake(m, 64, 32);
		propellerBlade = single(CubeListBuilder.create().texOffs(8, 18).addBox(-3.5F, -0.5F, -0.5F, 7, 1, 1, new CubeDeformation(0, -0.3F, -0.05F)), 64, 32);

		// jetpack: two tanks with nozzles on a frame; the stripes are tinted
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("tankL", CubeListBuilder.create().texOffs(0, 0).addBox(-3.4F, 1.5F, 2.3F, 2, 6, 2), PartPose.ZERO);
		r.addOrReplaceChild("tankR", CubeListBuilder.create().texOffs(0, 0).addBox(1.4F, 1.5F, 2.3F, 2, 6, 2), PartPose.ZERO);
		r.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 10).addBox(-3, 2.5F, 2.05F, 6, 2, 1), PartPose.ZERO);
		r.addOrReplaceChild("nozzleL", CubeListBuilder.create().texOffs(16, 0).addBox(-3.4F, 7.5F, 2.3F, 2, 1, 2, new CubeDeformation(-0.2F)), PartPose.ZERO);
		r.addOrReplaceChild("nozzleR", CubeListBuilder.create().texOffs(16, 0).addBox(1.4F, 7.5F, 2.3F, 2, 1, 2, new CubeDeformation(-0.2F)), PartPose.ZERO);
		jetpack = bake(m, 64, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("sL", CubeListBuilder.create().texOffs(24, 0).addBox(-3.4F, 3.5F, 2.3F, 2, 1, 2, new CubeDeformation(0.05F)), PartPose.ZERO);
		r.addOrReplaceChild("sR", CubeListBuilder.create().texOffs(24, 0).addBox(1.4F, 3.5F, 2.3F, 2, 1, 2, new CubeDeformation(0.05F)), PartPose.ZERO);
		jetpackStripes = bake(m, 64, 32);

		// guitar (body tinted, neck and head wooden); origin = middle of the body's back side
		guitarBody = single(CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3.5F, 0, 6, 7, 1), 64, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -10.5F, 0.1F, 1, 7, 1, new CubeDeformation(0, 0, -0.1F)), PartPose.ZERO);
		r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 0).addBox(-1, -12.5F, 0, 2, 2, 1), PartPose.ZERO);
		guitarNeck = bake(m, 64, 32);

		// sword on the back: blade pointing down (+y) from the guard at the origin, hilt above
		swordBlade = single(CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 1, -0.5F, 1, 11, 1, new CubeDeformation(0, 0, -0.2F)), 32, 16);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("guard", CubeListBuilder.create().texOffs(8, 0).addBox(-2, 0, -0.5F, 4, 1, 1), PartPose.ZERO);
		r.addOrReplaceChild("grip", CubeListBuilder.create().texOffs(8, 4).addBox(-0.5F, -3, -0.5F, 1, 3, 1, new CubeDeformation(-0.05F)), PartPose.ZERO);
		r.addOrReplaceChild("pommel", CubeListBuilder.create().texOffs(8, 10).addBox(-0.5F, -4, -0.5F, 1, 1, 1, new CubeDeformation(0.05F)), PartPose.ZERO);
		swordHilt = bake(m, 32, 16);
	}

	private static ModelPart single(CubeListBuilder cubes, int w, int h) {
		MeshDefinition m = new MeshDefinition();
		m.getRoot().addOrReplaceChild("p", cubes, PartPose.ZERO);
		return bake(m, w, h);
	}

	/** Models of the 1.6.0 cosmetics (head space for hats, body space for extras). */
	private static void buildV16() {
		MeshDefinition m = new MeshDefinition();
		PartDefinition r = m.getRoot();
		r.addOrReplaceChild("l1", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -10, -3, 6, 2, 6), PartPose.ZERO);
		r.addOrReplaceChild("l2", CubeListBuilder.create().texOffs(0, 8).addBox(-2, -12, -2, 4, 2, 4), PartPose.ZERO);
		r.addOrReplaceChild("l3", CubeListBuilder.create().texOffs(0, 14).addBox(-1, -14, -1, 2, 2, 2, new CubeDeformation(-0.2F)), PartPose.ZERO);
		partyHat = bake(m, 64, 32);
		partyPom = single(CubeListBuilder.create().texOffs(0, 18).addBox(-1, -15.6F, -1, 2, 2, 2, new CubeDeformation(-0.1F)), 64, 32);

		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-6.5F, -8.6F, -6.5F, 13, 1, 13), PartPose.ZERO);
		r.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -12.4F, -4.5F, 9, 4, 9, new CubeDeformation(0.12F)), PartPose.ZERO);
		cowboyHat = bake(m, 64, 64);
		cowboyBand = single(CubeListBuilder.create().texOffs(0, 32).addBox(-4.5F, -9.6F, -4.5F, 9, 1, 9, new CubeDeformation(0.25F)), 64, 64);

		bunnyEar = single(CubeListBuilder.create().texOffs(0, 0).addBox(-1, -6, -0.5F, 2, 6, 1), 32, 32);

		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -9.2F, -1, 10, 1, 2), PartPose.ZERO);
		r.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 4).addBox(-5, -8.2F, -1, 1, 5, 2), PartPose.ZERO);
		r.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 4).addBox(4, -8.2F, -1, 1, 5, 2), PartPose.ZERO);
		r.addOrReplaceChild("cupL", CubeListBuilder.create().texOffs(0, 12).addBox(-5.9F, -5.4F, -2, 2, 3, 4), PartPose.ZERO);
		r.addOrReplaceChild("cupR", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(3.9F, -5.4F, -2, 2, 3, 4), PartPose.ZERO);
		headphones = bake(m, 64, 32);

		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -5.2F, -4.8F, 9, 2, 1), PartPose.ZERO);
		r.addOrReplaceChild("tl", CubeListBuilder.create().texOffs(0, 4).addBox(-4.7F, -4.8F, -4.3F, 1, 1, 5), PartPose.ZERO);
		r.addOrReplaceChild("tr", CubeListBuilder.create().texOffs(0, 4).addBox(3.7F, -4.8F, -4.3F, 1, 1, 5), PartPose.ZERO);
		sunglasses = bake(m, 64, 32);

		vikingHelmet = single(CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -9.3F, -4.5F, 9, 3, 9, new CubeDeformation(0.2F)), 64, 32);
		vikingRim = single(CubeListBuilder.create().texOffs(0, 12).addBox(-5, -6.6F, -5, 10, 1, 10, SIDES), 64, 32);
		m = new MeshDefinition();
		PartDefinition base = m.getRoot().addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 24).addBox(-1, -3, -1, 2, 3, 2), PartPose.ZERO);
		base.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 24).addBox(-0.5F, -3, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0, -2.7F, 0, 0, 0, 0.55F));
		vikingHorn = bake(m, 64, 32);

		flowerVine = single(CubeListBuilder.create().texOffs(0, 0).addBox(-5, -9, -5, 10, 1, 10, SIDES), 64, 32);
		for (int g = 0; g < 3; g++) {
			m = new MeshDefinition();
			r = m.getRoot();
			for (int i = g; i < 12; i += 3) {
				double a = i * Math.PI * 2 / 12;
				float x = (float) (Math.cos(a) * 5.0);
				float z = (float) (Math.sin(a) * 5.0);
				r.addOrReplaceChild("f" + i, CubeListBuilder.create().texOffs(0, 14).addBox(x - 0.6F, -9.6F, z - 0.6F, 1, 1, 1, new CubeDeformation(0.25F)), PartPose.ZERO);
			}
			FLOWERS[g] = bake(m, 64, 32);
		}

		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -9.6F, -4.5F, 9, 4, 9, new CubeDeformation(0.25F)), PartPose.ZERO);
		r.addOrReplaceChild("fold", CubeListBuilder.create().texOffs(0, 14).addBox(-4.5F, -6.4F, -4.5F, 9, 2, 9, new CubeDeformation(0.5F)), PartPose.ZERO);
		beanie = bake(m, 64, 32);
		beaniePom = single(CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, -12.6F, -1.5F, 3, 3, 3), 64, 32);

		// extras (body space: neck at y 0, back at z +2)
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(0, 0).addBox(-3, 1, 2.1F, 6, 7, 3), PartPose.ZERO);
		r.addOrReplaceChild("pocket", CubeListBuilder.create().texOffs(0, 12).addBox(-2, 4.5F, 5.1F, 4, 3, 1), PartPose.ZERO);
		backpack = bake(m, 64, 32);
		m = new MeshDefinition();
		r = m.getRoot();
		r.addOrReplaceChild("sl", CubeListBuilder.create().texOffs(0, 18).addBox(-3, -0.1F, -2.35F, 1, 6, 1, new CubeDeformation(-0.2F)), PartPose.ZERO);
		r.addOrReplaceChild("sr", CubeListBuilder.create().texOffs(0, 18).addBox(2, -0.1F, -2.35F, 1, 6, 1, new CubeDeformation(-0.2F)), PartPose.ZERO);
		r.addOrReplaceChild("tl", CubeListBuilder.create().texOffs(0, 18).addBox(-3, -0.4F, -2, 1, 1, 4, new CubeDeformation(-0.2F)), PartPose.ZERO);
		r.addOrReplaceChild("tr", CubeListBuilder.create().texOffs(0, 18).addBox(2, -0.4F, -2, 1, 1, 4, new CubeDeformation(-0.2F)), PartPose.ZERO);
		backpackStraps = bake(m, 64, 32);

		tailThin = single(CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, 0, 1, 1, 3), 32, 32);
		tailFluffy = single(CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, -1.5F, 0, 3, 3, 3), 32, 32);
		tailFluffyTip = single(CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -1.5F, 0, 3, 3, 3), 32, 32);
		tailArrow = single(CubeListBuilder.create().texOffs(0, 22).addBox(-1, -1, 0, 2, 2, 1), 32, 32);

		scarf = single(CubeListBuilder.create().texOffs(0, 0).addBox(-4.8F, -0.2F, -2.8F, 9.6F, 2.5F, 5.6F, SIDES), 64, 32);
		scarfEnd = single(CubeListBuilder.create().texOffs(0, 10).addBox(-1, 0, -0.5F, 2, 5, 1), 64, 32);
	}
}
