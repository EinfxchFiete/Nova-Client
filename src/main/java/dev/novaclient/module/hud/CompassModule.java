package dev.novaclient.module.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** A horizontal compass bar like in many PvP clients. */
public class CompassModule extends HudModule {
	private static final int W = 160;
	private static final int H = 16;
	private static final String[] LABELS = {"S", "SW", "W", "NW", "N", "NO", "O", "SO"};

	public CompassModule() {
		super("Kompassleiste", "Himmelsrichtungen als Leiste oben am Bildschirm", false, Anchor.CENTER, 0, Anchor.START, 22);
	}

	@Override
	public int getWidth(boolean editor) {
		return W;
	}

	@Override
	public int getHeight(boolean editor) {
		return H;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		float yaw = mc().player == null ? 180F : Mth.wrapDegrees(mc().player.getYRot());
		int center = x + W / 2;
		float pxPerDeg = 1.2F;
		g.enableScissor(x, y, x + W, y + H);
		for (int deg = -180; deg < 540; deg += 15) {
			float diff = Mth.wrapDegrees(deg - yaw);
			int px = Math.round(center + diff * pxPerDeg);
			if (px < x - 10 || px > x + W + 10) {
				continue;
			}
			int norm = Math.floorMod(deg, 360);
			if (norm % 45 == 0) {
				String label = LABELS[norm / 45];
				boolean main = norm % 90 == 0;
				int color = label.equals("N") ? 0xFFFF5555 : main ? 0xFFFFFFFF : 0xFFB0B0BC;
				text(g, label, px - font().width(label) / 2, y + 4, color);
			} else {
				g.fill(px, y + 6, px + 1, y + 10, 0x90FFFFFF);
			}
		}
		g.disableScissor();
		g.fill(center, y + H - 3, center + 1, y + H, ThemeModule.accent());
		g.fill(center - 1, y + H - 2, center + 2, y + H - 1, ThemeModule.accent());
	}
}
