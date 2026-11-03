package dev.novaclient.module.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.novaclient.NovaClient;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.world.Waypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.joml.Matrix3x2fStack;

/**
 * A small map of the area around you (north is up), drawn from the block colours of the loaded chunks,
 * with you as an arrow, other players and your waypoints. Updated twice per second, only from what your
 * client already knows (like a paper map).
 */
public class MinimapModule extends HudModule {
	private static final int RES = 64;
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "dynamic/minimap");

	public final NumberSetting size = add(new NumberSetting("Größe", 80, 48, 140, 4));
	public final ModeSetting zoom = add(new ModeSetting("Zoom", "Normal", "Nah", "Normal", "Weit"));
	public final BoolSetting players = add(new BoolSetting("Spieler anzeigen", true));
	public final BoolSetting waypoints = add(new BoolSetting("Wegpunkte anzeigen", true));
	public final BoolSetting coords = add(new BoolSetting("Koordinaten darunter", true));

	private DynamicTexture texture;
	private int ticks;
	private int centerX;
	private int centerZ;

	public MinimapModule() {
		super("Minimap", "Kleine Karte der Umgebung mit dir, Spielern und Wegpunkten", false, Anchor.END, 2, Anchor.START, 190);
	}

	/** Blocks per map pixel. */
	private int scale() {
		return zoom.is("Nah") ? 1 : zoom.is("Weit") ? 4 : 2;
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		if (!isEnabled() || mc.level == null || mc.player == null) {
			return;
		}
		if (++ticks < 10 && texture != null) {
			return;
		}
		ticks = 0;
		update(mc.level, mc.player);
	}

	private void update(ClientLevel level, LocalPlayer p) {
		if (texture == null) {
			texture = new DynamicTexture(() -> "Nova minimap", RES, RES, false);
			Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
		}
		NativeImage img = texture.getPixels();
		if (img == null) {
			return;
		}
		int s = scale();
		centerX = p.getBlockX();
		centerZ = p.getBlockZ();
		boolean ceiling = level.dimensionType().hasCeiling();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int[] heights = new int[RES * RES];
		for (int py = 0; py < RES; py++) {
			for (int px = 0; px < RES; px++) {
				int x = centerX + (px - RES / 2) * s;
				int z = centerZ + (py - RES / 2) * s;
				int argb = 0xFF101014;
				int top = Integer.MIN_VALUE;
				if (level.hasChunk(x >> 4, z >> 4)) {
					int y = ceiling ? Math.min(p.getBlockY() + 2, level.getMaxY()) : level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
					int min = Math.max(level.getMinY(), y - 64);
					for (; y >= min; y--) {
						pos.set(x, y, z);
						BlockState st = level.getBlockState(pos);
						MapColor mcol = st.getMapColor(level, pos);
						if (mcol != MapColor.NONE) {
							top = y;
							argb = mcol.calculateARGBColor(MapColor.Brightness.NORMAL);
							break;
						}
					}
				}
				heights[py * RES + px] = top;
				img.setPixel(px, py, argb | 0xFF000000);
			}
		}
		// relief: darker when lower than the pixel north of it, brighter when higher
		for (int py = 1; py < RES; py++) {
			for (int px = 0; px < RES; px++) {
				int h = heights[py * RES + px];
				int n = heights[(py - 1) * RES + px];
				if (h == Integer.MIN_VALUE || n == Integer.MIN_VALUE || h == n) {
					continue;
				}
				int c = img.getPixel(px, py);
				float f = h > n ? 1.12F : 0.82F;
				int r = Math.min(255, (int) (((c >> 16) & 0xFF) * f));
				int g = Math.min(255, (int) (((c >> 8) & 0xFF) * f));
				int b = Math.min(255, (int) ((c & 0xFF) * f));
				img.setPixel(px, py, 0xFF000000 | r << 16 | g << 8 | b);
			}
		}
		texture.upload();
	}

	@Override
	public int getWidth(boolean editor) {
		return size.getInt() + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return size.getInt() + PADDING * 2 + (coords.isOn() ? 11 : 0);
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		Minecraft mc = mc();
		int sz = size.getInt();
		int mx = x + PADDING;
		int my = y + PADDING;
		LocalPlayer p = mc.player;
		if (texture == null || p == null) {
			g.fill(mx, my, mx + sz, my + sz, 0xFF1A1A22);
			g.centeredText(font(), "Karte", mx + sz / 2, my + sz / 2 - 4, 0xFF9A9AAE);
			return;
		}
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, mx, my, 0, 0, sz, sz, RES, RES, RES, RES);
		int accent = ThemeModule.accent();
		g.fill(mx, my, mx + sz, my + 1, accent);
		g.fill(mx, my + sz - 1, mx + sz, my + sz, accent);
		g.fill(mx, my, mx + 1, my + sz, accent);
		g.fill(mx + sz - 1, my, mx + sz, my + sz, accent);
		g.text(font(), "N", mx + sz / 2 - 2, my + 2, 0xFFFFFFFF, true);
		double pixelPerBlock = sz / (double) (RES * scale());
		if (waypoints.isOn()) {
			for (Waypoints.Waypoint w : Waypoints.here(mc)) {
				dot(g, mx, my, sz, (w.x() + 0.5 - centerX) * pixelPerBlock, (w.z() + 0.5 - centerZ) * pixelPerBlock, w.color(), 2);
			}
		}
		if (players.isOn() && mc.level != null) {
			for (AbstractClientPlayer other : mc.level.players()) {
				if (other != p && !other.isInvisible()) {
					dot(g, mx, my, sz, (other.getX() - centerX) * pixelPerBlock, (other.getZ() - centerZ) * pixelPerBlock, 0xFFFFFFFF, 1);
				}
			}
		}
		// you: an arrow in the middle, turned to where you look
		float cx = mx + sz / 2F + (float) ((p.getX() - centerX - 0.5) * pixelPerBlock);
		float cy = my + sz / 2F + (float) ((p.getZ() - centerZ - 0.5) * pixelPerBlock);
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.rotate((float) Math.toRadians(p.getYRot() + 180));
		for (int i = 0; i < 4; i++) {
			g.fill(-i, -3 + i * 2, i + 1, -1 + i * 2, 0xFF000000);
			g.fill(-i, -4 + i * 2, i + 1, -2 + i * 2, accent | 0xFF000000);
		}
		pose.popMatrix();
		if (coords.isOn()) {
			String c = p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ();
			g.centeredText(font(), c, mx + sz / 2, my + sz + 2, 0xFFFFFFFF);
		}
	}

	private static void dot(GuiGraphicsExtractor g, int mx, int my, int sz, double dx, double dz, int color, int r) {
		int px = (int) Math.round(mx + sz / 2.0 + dx);
		int py = (int) Math.round(my + sz / 2.0 + dz);
		px = Math.max(mx + 1 + r, Math.min(mx + sz - 2 - r, px));
		py = Math.max(my + 1 + r, Math.min(my + sz - 2 - r, py));
		g.fill(px - r - 1, py - r - 1, px + r + 2, py + r + 2, 0xFF000000);
		g.fill(px - r, py - r, px + r + 1, py + r + 1, color);
	}
}
