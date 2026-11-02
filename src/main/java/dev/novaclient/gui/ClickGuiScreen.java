package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** The main menu of the client (default key: Right Shift). */
public class ClickGuiScreen extends Screen {
	private static final int SIDEBAR = 96;
	private static final int MODULE_ROW = 24;
	private static final int SETTING_ROW = 18;
	private static final int ROW_GAP = 2;

	private static Category lastCategory = Category.HUD;

	private final @Nullable Screen parent;
	private Category category = lastCategory;
	private @Nullable Module expanded;
	private @Nullable Module listening;
	private @Nullable NumberSetting dragging;
	private int draggingSliderX;
	private int draggingSliderW;
	private double scroll;

	// layout, recomputed every frame
	private int left;
	private int top;
	private int winW;
	private int winH;

	private record Row(int y, int height, Module module, @Nullable Setting<?> setting, boolean keybind) {
	}

	public ClickGuiScreen(@Nullable Screen parent) {
		super(Component.literal(NovaClient.NAME));
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

	private void layout() {
		winW = Math.min(420, width - 16);
		winH = Math.min(270, height - 16);
		left = (width - winW) / 2;
		top = (height - winH) / 2;
	}

	private int contentX() {
		return left + SIDEBAR + 8;
	}

	private int contentW() {
		return winW - SIDEBAR - 16;
	}

	private int contentTop() {
		return top + 30;
	}

	private int contentBottom() {
		return top + winH - 8;
	}

	private List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		int y = contentTop() - (int) scroll;
		for (Module m : NovaClient.modules().getModules(category)) {
			rows.add(new Row(y, MODULE_ROW, m, null, false));
			y += MODULE_ROW + ROW_GAP;
			if (m == expanded) {
				for (Setting<?> s : m.getSettings()) {
					rows.add(new Row(y, SETTING_ROW, m, s, false));
					y += SETTING_ROW + ROW_GAP;
				}
				rows.add(new Row(y, SETTING_ROW, m, null, true));
				y += SETTING_ROW + ROW_GAP;
			}
		}
		return rows;
	}

	private double maxScroll() {
		List<Row> rows = rows();
		if (rows.isEmpty()) {
			return 0;
		}
		Row last = rows.get(rows.size() - 1);
		int contentHeight = (int) (last.y() + last.height() + scroll) - contentTop();
		return Math.max(0, contentHeight - (contentBottom() - contentTop()));
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		layout();
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
		int accent = ThemeModule.accent();

		g.fill(left - 1, top - 1, left + winW + 1, top + winH + 1, (accent & 0x00FFFFFF) | 0x80000000);
		g.fill(left, top, left + winW, top + winH, 0xF0101016);
		g.fill(left, top, left + SIDEBAR, top + winH, 0xF016161E);

		g.text(font, "NOVA", left + 10, top + 10, accent, true);
		g.text(font, "CLIENT", left + 10 + font.width("NOVA "), top + 10, 0xFFFFFFFF, true);

		Category[] cats = Category.values();
		for (int i = 0; i < cats.length; i++) {
			int y = top + 32 + i * 22;
			boolean selected = cats[i] == category;
			boolean hover = inside(mouseX, mouseY, left + 6, y, left + SIDEBAR - 6, y + 18);
			if (selected) {
				g.fill(left + 6, y, left + SIDEBAR - 6, y + 18, (accent & 0x00FFFFFF) | 0x40000000);
				g.fill(left + 6, y, left + 8, y + 18, accent);
			} else if (hover) {
				g.fill(left + 6, y, left + SIDEBAR - 6, y + 18, 0x20FFFFFF);
			}
			g.text(font, cats[i].getDisplayName(), left + 14, y + 5, selected ? 0xFFFFFFFF : 0xFFB0B0BC, false);
		}

		int by = top + winH - 26;
		boolean hoverEditor = inside(mouseX, mouseY, left + 6, by, left + SIDEBAR - 6, by + 18);
		g.fill(left + 6, by, left + SIDEBAR - 6, by + 18, hoverEditor ? (accent & 0x00FFFFFF) | 0x70000000 : (accent & 0x00FFFFFF) | 0x38000000);
		g.centeredText(font, "HUD-Editor", left + SIDEBAR / 2, by + 5, 0xFFFFFFFF);

		int cx = contentX();
		int cw = contentW();
		g.text(font, category.getDisplayName(), cx, top + 11, 0xFFFFFFFF, true);
		String hint = "Linksklick: an/aus  ·  Rechtsklick: Einstellungen";
		g.text(font, hint, cx + cw - font.width(hint), top + 11, 0xFF6C6C7A, false);

		g.enableScissor(cx, contentTop(), cx + cw, contentBottom());
		for (Row row : rows()) {
			if (row.y() + row.height() < contentTop() || row.y() > contentBottom()) {
				continue;
			}
			boolean hover = mouseY >= contentTop() && mouseY < contentBottom()
				&& inside(mouseX, mouseY, cx, row.y(), cx + cw, row.y() + row.height());
			if (row.setting() == null && !row.keybind()) {
				renderModuleRow(g, row, cx, cw, hover, accent);
			} else {
				renderSettingRow(g, row, cx, cw, hover, accent, mouseX);
			}
		}
		g.disableScissor();

		double max = maxScroll();
		if (max > 0) {
			int trackH = contentBottom() - contentTop();
			int barH = Math.max(20, (int) (trackH * trackH / (trackH + max)));
			int barY = contentTop() + (int) ((trackH - barH) * (scroll / max));
			g.fill(left + winW - 5, barY, left + winW - 3, barY + barH, 0x80FFFFFF);
		}
	}

	private void renderModuleRow(GuiGraphicsExtractor g, Row row, int cx, int cw, boolean hover, int accent) {
		Module m = row.module();
		int y = row.y();
		g.fill(cx, y, cx + cw, y + MODULE_ROW, hover ? 0xFF24242E : 0xFF1B1B23);
		if (m.isEnabled()) {
			g.fill(cx, y, cx + 2, y + MODULE_ROW, accent);
		}
		g.text(font, m.getName(), cx + 8, y + 3, m.isEnabled() ? 0xFFFFFFFF : 0xFFB8B8C4, false);
		String desc = font.plainSubstrByWidth(m.getDescription(), cw - 70);
		g.text(font, desc, cx + 8, y + 13, 0xFF787886, false);
		g.text(font, m == expanded ? "-" : "+", cx + cw - 44, y + 8, 0xFF9090A0, false);
		drawSwitch(g, cx + cw - 30, y + 7, m.isEnabled(), accent);
	}

	private void renderSettingRow(GuiGraphicsExtractor g, Row row, int cx, int cw, boolean hover, int accent, int mouseX) {
		int y = row.y();
		int x0 = cx + 12;
		g.fill(x0, y, cx + cw, y + SETTING_ROW, hover ? 0xFF1E1E27 : 0xFF15151C);
		int ty = y + 5;
		if (row.keybind()) {
			g.text(font, "Tastenbelegung", x0 + 6, ty, 0xFFD0D0DA, false);
			String label;
			if (listening == row.module()) {
				label = "> Taste drücken <";
			} else if (row.module().getKeybind() <= 0) {
				label = "Keine";
			} else {
				label = keyName(row.module().getKeybind());
			}
			int bw = font.width(label) + 10;
			int bx = cx + cw - 8 - bw;
			g.fill(bx, y + 2, bx + bw, y + SETTING_ROW - 2, listening == row.module() ? (accent & 0x00FFFFFF) | 0x60000000 : 0xFF2A2A34);
			g.text(font, label, bx + 5, ty, 0xFFFFFFFF, false);
			return;
		}
		Setting<?> s = row.setting();
		g.text(font, s.getName(), x0 + 6, ty, 0xFFD0D0DA, false);
		if (s instanceof BoolSetting b) {
			drawSwitch(g, cx + cw - 30, y + 4, b.isOn(), accent);
		} else if (s instanceof NumberSetting n) {
			int sx = sliderX(cx, cw);
			int sw = sliderW();
			double t = (n.get() - n.getMin()) / (n.getMax() - n.getMin());
			int fillTo = sx + (int) Math.round(sw * t);
			g.fill(sx, y + 8, sx + sw, y + 10, 0xFF33333F);
			g.fill(sx, y + 8, fillTo, y + 10, accent);
			g.fill(fillTo - 2, y + 5, fillTo + 2, y + 13, 0xFFFFFFFF);
			String v = n.format();
			g.text(font, v, sx - 6 - font.width(v), ty, 0xFF9A9AA8, false);
		}
	}

	private void drawSwitch(GuiGraphicsExtractor g, int x, int y, boolean on, int accent) {
		g.fill(x, y, x + 22, y + 10, on ? accent : 0xFF3A3A46);
		int knobX = on ? x + 13 : x + 1;
		g.fill(knobX, y + 1, knobX + 8, y + 9, 0xFFFFFFFF);
	}

	private int sliderX(int cx, int cw) {
		return cx + cw - 8 - sliderW();
	}

	private static int sliderW() {
		return 90;
	}

	private static String keyName(int key) {
		return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
	}

	private static boolean inside(double mx, double my, int x0, int y0, int x1, int y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1;
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		layout();
		double mx = event.x();
		double my = event.y();
		int button = event.button();

		if (listening != null) {
			listening = null;
			return true;
		}

		Category[] cats = Category.values();
		for (int i = 0; i < cats.length; i++) {
			int y = top + 32 + i * 22;
			if (inside(mx, my, left + 6, y, left + SIDEBAR - 6, y + 18)) {
				category = cats[i];
				lastCategory = category;
				expanded = null;
				scroll = 0;
				return true;
			}
		}
		int by = top + winH - 26;
		if (inside(mx, my, left + 6, by, left + SIDEBAR - 6, by + 18)) {
			NovaClient.config().save();
			minecraft.gui.setScreen(new HudEditorScreen(this));
			return true;
		}

		int cx = contentX();
		int cw = contentW();
		if (my >= contentTop() && my < contentBottom()) {
			for (Row row : rows()) {
				if (!inside(mx, my, cx, row.y(), cx + cw, row.y() + row.height())) {
					continue;
				}
				Module m = row.module();
				if (row.keybind()) {
					listening = m;
				} else if (row.setting() == null) {
					boolean onPlus = mx >= cx + cw - 48 && mx < cx + cw - 34;
					if (button == 1 || onPlus) {
						expanded = expanded == m ? null : m;
					} else if (button == 0) {
						m.toggle();
					}
				} else if (row.setting() instanceof BoolSetting b) {
					b.toggle();
				} else if (row.setting() instanceof NumberSetting n) {
					dragging = n;
					draggingSliderX = sliderX(cx, cw);
					draggingSliderW = sliderW();
					updateSlider(mx);
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void updateSlider(double mx) {
		if (dragging == null) {
			return;
		}
		double t = (mx - draggingSliderX) / draggingSliderW;
		t = Math.max(0, Math.min(1, t));
		dragging.set(dragging.getMin() + t * (dragging.getMax() - dragging.getMin()));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			updateSlider(event.x());
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
		layout();
		scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 18));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listening != null) {
			int key = event.key();
			if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
				listening.setKeybind(0);
			} else {
				listening.setKeybind(key);
			}
			listening = null;
			return true;
		}
		if (event.key() == InputConstants.KEY_RSHIFT) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}
}
