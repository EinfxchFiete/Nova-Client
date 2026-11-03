package dev.novaclient.module.hud;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small slot numbers 1-9 on the hotbar. */
public class HotbarNumbersModule extends Module {
	private static HotbarNumbersModule instance;

	public HotbarNumbersModule() {
		super("Hotbar-Nummern", "Kleine Nummern 1–9 auf den Hotbar-Feldern", Category.HUD, false);
		instance = this;
	}

	/** Draws the numbers (called from the HUD). */
	public static void render(GuiGraphicsExtractor g, int screenW, int screenH) {
		if (instance == null || !instance.isEnabled()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.isSpectator()) {
			return;
		}
		int x0 = screenW / 2 - 91;
		int y = screenH - 21;
		int selected = mc.player.getInventory().getSelectedSlot();
		for (int i = 0; i < 9; i++) {
			int x = x0 + i * 20 + 3;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(0.6F, 0.6F);
			g.text(mc.font, String.valueOf(i + 1), 0, 0, i == selected ? ThemeModule.accent() : 0xFFDDDDDD, true);
			g.pose().popMatrix();
		}
	}
}
