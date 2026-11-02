package dev.novaclient.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing helpers for the menus and HUD. */
public final class Render {
	private Render() {
	}

	/** Rectangle with 2px rounded corners. Uses non-overlapping fills so translucent colours stay even. */
	public static void rounded(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		if (x1 - x0 < 4 || y1 - y0 < 4) {
			g.fill(x0, y0, x1, y1, color);
			return;
		}
		g.fill(x0 + 2, y0, x1 - 2, y0 + 1, color);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, color);
		g.fill(x0, y0 + 2, x1, y1 - 2, color);
		g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, color);
		g.fill(x0 + 2, y1 - 1, x1 - 2, y1, color);
	}

	/** 1px rounded outline. */
	public static void roundedOutline(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		g.fill(x0 + 2, y0, x1 - 2, y0 + 1, color);
		g.fill(x0 + 2, y1 - 1, x1 - 2, y1, color);
		g.fill(x0, y0 + 2, x0 + 1, y1 - 2, color);
		g.fill(x1 - 1, y0 + 2, x1, y1 - 2, color);
		g.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 2, color);
		g.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 2, color);
		g.fill(x0 + 1, y1 - 2, x0 + 2, y1 - 1, color);
		g.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, color);
	}

	/** Replaces the alpha channel of an ARGB colour. */
	public static int alpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
	}

	/** Linear blend of two ARGB colours. */
	public static int mix(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
			| (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}

	/** iOS-style toggle switch, 20x10. */
	public static void toggle(GuiGraphicsExtractor g, int x, int y, boolean on, int accent) {
		rounded(g, x, y, x + 20, y + 10, on ? accent : 0xFF3A3A48);
		int kx = on ? x + 11 : x + 1;
		rounded(g, kx, y + 1, kx + 8, y + 9, 0xFFFFFFFF);
	}

	public static boolean inside(double mx, double my, int x0, int y0, int x1, int y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1;
	}
}
