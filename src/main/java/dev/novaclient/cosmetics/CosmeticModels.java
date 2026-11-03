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
	}
}
