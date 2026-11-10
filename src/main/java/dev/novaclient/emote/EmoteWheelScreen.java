package dev.novaclient.emote;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.util.Render;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Round emote menu: click an emote to play it. */
public class EmoteWheelScreen extends Screen {
	private static final int RADIUS = 84;
	private static final int ITEM_W = 76;
	private static final int ITEM_H = 22;

	private static final int PER_PAGE = 8;
	private static int page;
	private final List<String> all = new ArrayList<>(Emotes.ALL.keySet());
	private List<String> ids = pageIds();

	private int pages() {
		return (all.size() + PER_PAGE - 1) / PER_PAGE;
	}

	private List<String> pageIds() {
		page = Math.floorMod(page, pages());
		return all.subList(page * PER_PAGE, Math.min(all.size(), (page + 1) * PER_PAGE));
	}

	/** Switches to the next (+1) or previous (-1) page of emotes. */
	public void turnPage(int by) {
		page = Math.floorMod(page + by, pages());
		ids = pageIds();
	}

	public EmoteWheelScreen() {
		super(Component.literal("Emotes"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x50000000);
	}

	private int[] center(int i) {
		double a = -Math.PI / 2 + i * Math.PI * 2 / ids.size();
		return new int[] {width / 2 + (int) Math.round(Math.cos(a) * RADIUS), height / 2 + (int) Math.round(Math.sin(a) * RADIUS)};
	}

	/** Index of the emote under the mouse (by direction from the centre), or -1. */
	public int hovered(double mx, double my) {
		double dx = mx - width / 2.0;
		double dy = my - height / 2.0;
		if (dx * dx + dy * dy < 18 * 18) {
			return -1;
		}
		double a = Math.atan2(dy, dx) + Math.PI / 2;
		double step = Math.PI * 2 / ids.size();
		int i = (int) Math.floor((a + step / 2) / step);
		return Math.floorMod(i, ids.size());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		int accent = ThemeModule.accent();
		int hover = hovered(mouseX, mouseY);
		int cx = width / 2;
		int cy = height / 2;
		Render.rounded(g, cx - 26, cy - 10, cx + 26, cy + 10, 0xE0101016);
		g.centeredText(font, "Seite " + (page + 1) + "/" + pages(), cx, cy - 4, accent);
		for (int i = 0; i < ids.size(); i++) {
			int[] c = center(i);
			Emotes.Info info = Emotes.ALL.get(ids.get(i));
			boolean h = i == hover;
			Render.rounded(g, c[0] - ITEM_W / 2, c[1] - ITEM_H / 2, c[0] + ITEM_W / 2, c[1] + ITEM_H / 2,
				h ? Render.alpha(accent, 0xE0) : 0xE0181820);
			if (h) {
				Render.roundedOutline(g, c[0] - ITEM_W / 2, c[1] - ITEM_H / 2, c[0] + ITEM_W / 2, c[1] + ITEM_H / 2, 0xFFFFFFFF);
			}
			g.centeredText(font, info.name(), c[0], c[1] - 4, 0xFFFFFFFF);
		}
		g.centeredText(font, "Klicke ein Emote an  ·  Mausrad oder Mitte = nächste Seite  ·  ESC = schließen", cx, cy + RADIUS + 26, 0xFFC0C0CC);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int i = hovered(event.x(), event.y());
			if (i < 0) {
				turnPage(1);
				return true;
			}
			if (i >= 0) {
				minecraft.gui.setScreen(null);
				Emotes.play(ids.get(i));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		turnPage(scrollY < 0 ? 1 : -1);
		return true;
	}

	/** Screen position of an emote button (used by tests). */
	public int[] buttonCenter(String id) {
		page = all.indexOf(id) / PER_PAGE;
		ids = pageIds();
		return center(ids.indexOf(id));
	}

	public static Map<String, Emotes.Info> all() {
		return Emotes.ALL;
	}
}
