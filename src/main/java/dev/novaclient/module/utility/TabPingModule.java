package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Shows the ping in the tab list as a number (ms) instead of signal bars. */
public class TabPingModule extends Module {
	private static TabPingModule instance;

	public TabPingModule() {
		super("Tab-Ping in ms", "Ping in der Tab-Liste als Zahl statt Balken", Category.UTILITY, true);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Extra slot width so the number never overlaps the name (vanilla reserves 13 px for the bars). */
	public static final int SLOT_EXTRA = 26;

	/** Draws the ping text right-aligned in the tab list slot. */
	public static void draw(GuiGraphicsExtractor g, int slotWidth, int xo, int yo, PlayerInfo info) {
		Minecraft mc = Minecraft.getInstance();
		boolean self = mc.player != null && info.getProfile().id().equals(mc.player.getUUID());
		int ms = self ? dev.novaclient.util.PingTracker.current(mc) : info.getLatency();
		String text = ms < 0 ? "?" : ms + "ms";
		int color = ms < 0 ? 0xFFAAAAAA : ms < 80 ? 0xFF55FF55 : ms < 150 ? 0xFFA0E040 : ms < 300 ? 0xFFFFDD44 : 0xFFFF5555;
		var font = mc.font;
		g.pose().pushMatrix();
		g.pose().translate(xo + slotWidth - 1, yo + 1.5F);
		g.pose().scale(0.75F, 0.75F);
		g.text(font, text, -font.width(text), 0, color, true);
		g.pose().popMatrix();
	}
}
