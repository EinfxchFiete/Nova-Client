package dev.novaclient.module.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/** Shows the 27 main inventory slots on the HUD. */
public class InventoryPreviewModule extends HudModule {
	public InventoryPreviewModule() {
		super("Inventar-Vorschau", "Dein Inventar (ohne Hotbar) immer im Blick", false, Anchor.END, 2, Anchor.END, 96);
	}

	@Override
	public int getWidth(boolean editor) {
		return 9 * 18 + PADDING * 2 - 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return 3 * 18 + PADDING * 2 - 2;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		LocalPlayer p = mc().player;
		for (int i = 0; i < 27; i++) {
			int sx = x + PADDING + (i % 9) * 18;
			int sy = y + PADDING + (i / 9) * 18;
			g.fill(sx, sy, sx + 16, sy + 16, 0x40FFFFFF);
			if (p != null) {
				ItemStack s = p.getInventory().getItem(9 + i);
				if (!s.isEmpty()) {
					g.item(s, sx, sy);
					g.itemDecorations(font(), s, sx, sy);
				}
			}
		}
	}
}
