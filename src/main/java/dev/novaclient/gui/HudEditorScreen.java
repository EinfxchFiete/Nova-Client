package dev.novaclient.gui;

import dev.novaclient.NovaClient;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.module.hud.ThemeModule;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Drag HUD elements around with the mouse. */
public class HudEditorScreen extends Screen {
	private static final int SNAP = 4;

	private final @Nullable Screen parent;
	private @Nullable HudModule dragging;
	private int offsetX;
	private int offsetY;

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

	private List<HudModule> visible() {
		return NovaClient.modules().getHudModules().stream().filter(HudModule::isEnabled).toList();
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

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		int accent = ThemeModule.accent();
		g.fill(width / 2, 0, width / 2 + 1, height, 0x20FFFFFF);
		g.fill(0, height / 2, width, height / 2 + 1, 0x20FFFFFF);

		HudModule hovered = dragging != null ? dragging : at(mouseX, mouseY);
		for (HudModule m : visible()) {
			m.render(g, width, height, true);
			int x = m.getScreenX(width, true);
			int y = m.getScreenY(height, true);
			int w = m.getWidth(true);
			int h = m.getHeight(true);
			if (w > 0 && h > 0) {
				g.outline(x - 1, y - 1, w + 2, h + 2, m == hovered ? accent : 0x60FFFFFF);
			}
		}
		if (hovered != null) {
			g.setTooltipForNextFrame(font, Component.literal(hovered.getName()), mouseX, mouseY);
		}

		g.centeredText(font, "HUD-Editor", width / 2, 8, accent);
		g.centeredText(font, "Ziehe die Elemente mit der Maus  ·  ESC = zurück", width / 2, 20, 0xFFC0C0C0);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			HudModule m = at(event.x(), event.y());
			if (m != null) {
				dragging = m;
				offsetX = (int) event.x() - m.getScreenX(width, true);
				offsetY = (int) event.y() - m.getScreenY(height, true);
				return true;
			}
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
}
