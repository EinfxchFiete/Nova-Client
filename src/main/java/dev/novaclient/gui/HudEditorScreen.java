package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.util.Render;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Drag HUD elements with the left mouse button. Right click an element to open its settings.
 * The "Elemente" button lists every HUD module so it can be switched on/off with one click.
 */
public class HudEditorScreen extends Screen {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;
	private static final int SNAP = 4;
	private static final int LIST_ROW = 14;
	private static final int LIST_W = 150;

	private final @Nullable Screen parent;
	private @Nullable HudModule dragging;
	private int offsetX;
	private int offsetY;
	private boolean listOpen;
	private int listScroll;

	public HudEditorScreen(@Nullable Screen parent) {
		super(Component.literal("HUD-Editor"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		NovaClient.config().save();
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x30000000);
	}

	private List<HudModule> all() {
		return NovaClient.modules().getHudModules();
	}

	private List<HudModule> visible() {
		return all().stream().filter(HudModule::isEnabled).toList();
	}

	private @Nullable HudModule at(double mx, double my) {
		List<HudModule> mods = visible();
		for (int i = mods.size() - 1; i >= 0; i--) {
			HudModule m = mods.get(i);
			int x = m.getScreenX(width, true);
			int y = m.getScreenY(height, true);
			if (mx >= x && mx < x + m.getWidth(true) && my >= y && my < y + m.getHeight(true)) {
				return m;
			}
		}
		return null;
	}

	// top bar: [Elemente] [Anordnen] [Fertig], each 68 wide with 4px gap
	private static final int BTN = 68;
	private static final String[] BAR = {"Elemente", "Anordnen", "Fertig"};

	private int barX() {
		return width / 2 - (BTN * 3 + 8) / 2;
	}

	private int buttonX(int i) {
		return barX() + i * (BTN + 4);
	}

	/** Stacks all enabled elements per screen side so nothing overlaps. */
	public void autoArrange() {
		for (HudModule.Anchor side : new HudModule.Anchor[] {HudModule.Anchor.START, HudModule.Anchor.END}) {
			List<HudModule> column = visible().stream()
				.filter(m -> m.getAnchorX() == side && m.getAnchorY() != HudModule.Anchor.END)
				.sorted(java.util.Comparator.comparingInt(m -> m.getScreenY(height, true)))
				.toList();
			int y = 2;
			for (HudModule m : column) {
				int w = m.getWidth(true);
				int x = side == HudModule.Anchor.START ? 2 : width - w - 2;
				m.setScreenPos(x, y, width, height, true);
				// keep the side even if the element is wide
				m.setPosition(side.name(), 2, m.getAnchorY().name(), m.getOffsetY());
				y += m.getHeight(true) + 2;
			}
		}
	}

	private int barY() {
		return 30;
	}

	private int listX() {
		return width / 2 - LIST_W / 2;
	}

	private int listY() {
		return barY() + 20;
	}

	private int listH() {
		return Math.min(all().size() * LIST_ROW + 6, height - listY() - 6);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		int accent = ThemeModule.accent();
		g.fill(width / 2, 0, width / 2 + 1, height, 0x18FFFFFF);
		g.fill(0, height / 2, width, height / 2 + 1, 0x18FFFFFF);

		boolean overUi = listOpen && Render.inside(mouseX, mouseY, listX(), listY(), listX() + LIST_W, listY() + listH())
			|| Render.inside(mouseX, mouseY, barX(), barY(), buttonX(2) + BTN, barY() + 16);
		HudModule hovered = dragging != null ? dragging : overUi ? null : at(mouseX, mouseY);
		for (HudModule m : visible()) {
			m.render(g, width, height, true);
			int x = m.getScreenX(width, true);
			int y = m.getScreenY(height, true);
			int w = m.getWidth(true);
			int h = m.getHeight(true);
			if (w > 0 && h > 0) {
				g.outline(x - 1, y - 1, w + 2, h + 2, m == hovered ? accent : 0x50FFFFFF);
			}
		}

		g.centeredText(font, "HUD-Editor", width / 2, 6, accent);
		g.centeredText(font, "Ziehen = verschieben  ·  Rechtsklick = Einstellungen", width / 2, 17, 0xFFB0B0BC);

		for (int i = 0; i < BAR.length; i++) {
			int bx = buttonX(i);
			boolean hover = Render.inside(mouseX, mouseY, bx, barY(), bx + BTN, barY() + 16);
			boolean on = hover || i == 0 && listOpen;
			Render.rounded(g, bx, barY(), bx + BTN, barY() + 16, on ? Render.alpha(accent, 0xC0) : 0xE0181820);
			g.centeredText(font, BAR[i], bx + BTN / 2, barY() + 4, 0xFFFFFFFF);
		}

		if (listOpen) {
			int lx = listX();
			int ly = listY();
			Render.rounded(g, lx, ly, lx + LIST_W, ly + listH(), 0xF0101016);
			Render.roundedOutline(g, lx, ly, lx + LIST_W, ly + listH(), Render.alpha(accent, 0x70));
			g.enableScissor(lx, ly, lx + LIST_W, ly + listH());
			List<HudModule> mods = all();
			for (int i = 0; i < mods.size(); i++) {
				HudModule m = mods.get(i);
				int ry = ly + 3 + i * LIST_ROW - listScroll;
				if (Render.inside(mouseX, mouseY, lx + 2, ry, lx + LIST_W - 2, ry + LIST_ROW)) {
					g.fill(lx + 2, ry, lx + LIST_W - 2, ry + LIST_ROW, 0x20FFFFFF);
				}
				g.text(font, m.getName(), lx + 8, ry + 3, m.isEnabled() ? 0xFFFFFFFF : 0xFF8A8A9A, false);
				Render.toggle(g, lx + LIST_W - 28, ry + 2, m.isEnabled(), accent);
			}
			g.disableScissor();
		}
		if (hovered != null && dragging == null) {
			g.setTooltipForNextFrame(font, Component.literal(hovered.getName()), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		int button = event.button();
		if (button == LEFT && Render.inside(mx, my, buttonX(0), barY(), buttonX(0) + BTN, barY() + 16)) {
			listOpen = !listOpen;
			return true;
		}
		if (button == LEFT && Render.inside(mx, my, buttonX(1), barY(), buttonX(1) + BTN, barY() + 16)) {
			autoArrange();
			return true;
		}
		if (button == LEFT && Render.inside(mx, my, buttonX(2), barY(), buttonX(2) + BTN, barY() + 16)) {
			onClose();
			return true;
		}
		if (listOpen && Render.inside(mx, my, listX(), listY(), listX() + LIST_W, listY() + listH())) {
			int i = (int) Math.floor((my - listY() - 3 + listScroll) / LIST_ROW);
			List<HudModule> mods = all();
			if (i >= 0 && i < mods.size()) {
				if (button == RIGHT) {
					minecraft.gui.setScreen(new ClickGuiScreen(this, mods.get(i)));
				} else {
					mods.get(i).toggle();
				}
			}
			return true;
		}
		HudModule m = at(mx, my);
		if (m != null && button == LEFT) {
			dragging = m;
			offsetX = (int) mx - m.getScreenX(width, true);
			offsetY = (int) my - m.getScreenY(height, true);
			return true;
		}
		if (m != null && button == RIGHT) {
			NovaClient.config().save();
			minecraft.gui.setScreen(new ClickGuiScreen(this, m));
			return true;
		}
		if (listOpen) {
			listOpen = false;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			int w = dragging.getWidth(true);
			int h = dragging.getHeight(true);
			int x = (int) event.x() - offsetX;
			int y = (int) event.y() - offsetY;
			if (Math.abs(x) < SNAP) {
				x = 0;
			}
			if (Math.abs(x + w - width) < SNAP) {
				x = width - w;
			}
			if (Math.abs(x + w / 2 - width / 2) < SNAP) {
				x = width / 2 - w / 2;
			}
			if (Math.abs(y) < SNAP) {
				y = 0;
			}
			if (Math.abs(y + h - height) < SNAP) {
				y = height - h;
			}
			if (Math.abs(y + h / 2 - height / 2) < SNAP) {
				y = height / 2 - h / 2;
			}
			dragging.setScreenPos(x, y, width, height, true);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (listOpen) {
			int max = Math.max(0, all().size() * LIST_ROW + 6 - listH());
			listScroll = (int) Math.max(0, Math.min(max, listScroll - scrollY * LIST_ROW));
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_ESCAPE && listOpen) {
			listOpen = false;
			return true;
		}
		return super.keyPressed(event);
	}

	public int[] buttonCenter(int i) {
		return new int[] {buttonX(i) + BTN / 2, barY() + 8};
	}

	public int[] listRowCenter(int index) {
		return new int[] {listX() + 40, listY() + 3 + index * LIST_ROW - listScroll + LIST_ROW / 2};
	}

	public boolean isListOpen() {
		return listOpen;
	}
}
