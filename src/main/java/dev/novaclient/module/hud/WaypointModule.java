package dev.novaclient.module.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.KeySetting;
import dev.novaclient.util.Notifications;
import dev.novaclient.util.Render;
import dev.novaclient.world.Waypoints;
import dev.novaclient.world.Waypoints.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Waypoints: press B to mark your position (as many as you like, saved per world/server and dimension).
 * They are shown as markers in the world, the HUD shows the nearest one. Manage them with /wegpunkt.
 */
public class WaypointModule extends TextHudModule {
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
	private static WaypointModule instance;

	public final KeySetting setKey = add(new KeySetting("Wegpunkt-Taste", InputConstants.KEY_B));
	public final BoolSetting markers = add(new BoolSetting("Markierungen in der Welt", true));
	public final BoolSetting coords = add(new BoolSetting("Koordinaten an der Markierung", false));

	public WaypointModule() {
		super("Wegpunkt", "Taste B setzt Wegpunkte (pro Welt gespeichert), Markierung in der Welt, /wegpunkt", true, Anchor.CENTER, 0, Anchor.START, 78);
		instance = this;
	}

	public static WaypointModule get() {
		return instance;
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		if (setKey.consumePress(mc.gui.screen() == null) && isEnabled() && p != null) {
			Waypoint w = Waypoints.add(mc, null);
			if (w != null) {
				Notifications.push(w.name() + " gesetzt: " + w.x() + ", " + w.y() + ", " + w.z(), 0);
			}
		}
	}

	private static String arrow(LocalPlayer p, Waypoint w) {
		double dx = w.x() + 0.5 - p.getX();
		double dz = w.z() + 0.5 - p.getZ();
		float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float rel = Mth.wrapDegrees(target - p.getYRot());
		return ARROWS[Math.floorMod(Math.round(rel / 45F), 8)];
	}

	@Override
	public int getWidth(boolean editor) {
		return !editor && Waypoints.nearest(mc()) == null ? 0 : super.getWidth(editor);
	}

	@Override
	public int getHeight(boolean editor) {
		return !editor && Waypoints.nearest(mc()) == null ? 0 : super.getHeight(editor);
	}

	@Override
	protected String label() {
		Waypoint w = mc().player == null ? null : Waypoints.nearest(mc());
		return w == null ? "Wegpunkt" : w.name();
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		Waypoint w = p == null ? null : Waypoints.nearest(mc());
		if (w == null) {
			return "Taste " + setKey.display() + " zum Setzen";
		}
		return Math.round(w.distanceTo(p)) + " m " + arrow(p, w);
	}

	/** Draws the markers of all waypoints in your dimension (called from the HUD). */
	public static void renderMarkers(GuiGraphicsExtractor g, int screenW, int screenH) {
		if (instance == null || !instance.isEnabled() || !instance.markers.isOn()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null) {
			return;
		}
		var font = mc.font;
		for (Waypoint w : Waypoints.here(mc)) {
			Vec3 ndc = mc.gameRenderer.projectPointToScreen(new Vec3(w.x() + 0.5, w.y() + 1.2, w.z() + 0.5));
			if (ndc.z > 1.0 || Math.abs(ndc.x) > 1.2 || Math.abs(ndc.y) > 1.2) {
				continue; // behind you or far outside of the screen
			}
			int sx = (int) Math.round((ndc.x + 1) / 2 * screenW);
			int sy = (int) Math.round((1 - ndc.y) / 2 * screenH);
			int dist = (int) Math.round(w.distanceTo(p));
			String text = w.name() + " · " + dist + " m";
			if (instance.coords.isOn()) {
				text += " (" + w.x() + ", " + w.y() + ", " + w.z() + ")";
			}
			int color = w.color();
			// diamond
			for (int i = 0; i < 5; i++) {
				g.fill(sx - i, sy - 4 + i, sx + i + 1, sy - 3 + i, color);
				g.fill(sx - i, sy + 4 - i, sx + i + 1, sy + 5 - i, color);
			}
			int tw = font.width(text);
			Render.rounded(g, sx - tw / 2 - 3, sy - 18, sx + tw / 2 + 3, sy - 7, 0xA0101016);
			g.text(font, text, sx - tw / 2, sy - 16, 0xFFFFFFFF, true);
		}
	}
}
