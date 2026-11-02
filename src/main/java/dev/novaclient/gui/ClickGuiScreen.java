package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.setting.Setting;
import dev.novaclient.util.Render;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Main menu (default key: Right Shift).
 * Grid page: click a card to toggle the module, right click or the menu icon opens its settings.
 */
public class ClickGuiScreen extends Screen {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;

	private static final int HEADER = 32;
	private static final int FOOTER = 26;
	private static final int CARD_H = 46;
	private static final int CARD_GAP = 6;
	private static final int ROW_H = 22;
	private static final int ROW_GAP = 4;
	private static final int SLIDER_W = 110;
	private static final int SEARCH_W = 110;

	private static final int BG = 0xF00E0E14;
	private static final int CARD = 0xFF181820;
	private static final int CARD_HOVER = 0xFF20202B;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_DIM = 0xFF8A8A9A;
	private static final int TEXT_MUTED = 0xFF5E5E6E;

	private static @Nullable Category lastCategory;

	private final @Nullable Screen parent;
	private @Nullable Category category = lastCategory;
	private @Nullable Module settingsModule;
	private @Nullable Module listening;
	private @Nullable NumberSetting dragging;
	private int dragSliderX;
	private String search = "";
	private boolean searchFocused;
	private double scroll;

	private int left;
	private int top;
	private int winW;
	private int winH;

	public ClickGuiScreen(@Nullable Screen parent) {
		this(parent, null);
	}

	public ClickGuiScreen(@Nullable Screen parent, @Nullable Module openSettings) {
		super(Component.literal(NovaClient.NAME));
		this.parent = parent;
		this.settingsModule = openSettings;
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

	// ------------------------------------------------------------------ layout helpers

	private void layout() {
		winW = Math.min(460, width - 24);
		winH = Math.min(300, height - 24);
		left = (width - winW) / 2;
		top = (height - winH) / 2;
	}

	private int bodyX() {
		return left + 12;
	}

	private int bodyW() {
		return winW - 24;
	}

	private int bodyTop() {
		return top + HEADER + 8;
	}

	private int bodyBottom() {
		return top + winH - FOOTER - 4;
	}

	private static String[] tabNames() {
		Category[] cats = Category.values();
		String[] out = new String[cats.length + 1];
		out[0] = "Alle";
		for (int i = 0; i < cats.length; i++) {
			out[i + 1] = cats[i].getDisplayName();
		}
		return out;
	}

	/** x positions of the tabs: [x0, x1] per tab. */
	private int[][] tabBounds() {
		String[] names = tabNames();
		int[][] out = new int[names.length][2];
		int x = left + 12 + font.width("NOVA CLIENT") + 16;
		for (int i = 0; i < names.length; i++) {
			int w = font.width(names[i]) + 14;
			out[i][0] = x;
			out[i][1] = x + w;
			x += w + 4;
		}
		return out;
	}

	private int searchX() {
		return left + winW - 12 - SEARCH_W;
	}

	private List<Module> visibleModules() {
		String q = search.trim().toLowerCase(Locale.ROOT);
		List<Module> out = new ArrayList<>();
		for (Module m : NovaClient.modules().getModules()) {
			if (category != null && m.getCategory() != category) {
				continue;
			}
			if (!q.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(q)
				&& !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
				continue;
			}
			out.add(m);
		}
		return out;
	}

	private int columns() {
		return bodyW() >= 400 ? 3 : 2;
	}

	private int cardW() {
		int cols = columns();
		return (bodyW() - CARD_GAP * (cols - 1)) / cols;
	}

	private int cardX(int index) {
		return bodyX() + (index % columns()) * (cardW() + CARD_GAP);
	}

	private int cardY(int index) {
		return bodyTop() + (index / columns()) * (CARD_H + CARD_GAP) - (int) scroll;
	}

	/** Settings rows of the open module, including the extra keybind/HUD rows. */
	private List<Object> settingRows() {
		List<Object> rows = new ArrayList<>(settingsModule.getSettings());
		rows.add("keybind");
		if (settingsModule instanceof HudModule) {
			rows.add("hudeditor");
		}
		return rows;
	}

	private int settingsListTop() {
		return bodyTop() + 40;
	}

	private int rowY(int index) {
		return settingsListTop() + index * (ROW_H + ROW_GAP) - (int) scroll;
	}

	private double maxScroll() {
		int contentH;
		int viewH;
		if (settingsModule != null) {
			contentH = settingRows().size() * (ROW_H + ROW_GAP);
			viewH = bodyBottom() - settingsListTop();
		} else {
			int rows = (visibleModules().size() + columns() - 1) / columns();
			contentH = rows * (CARD_H + CARD_GAP);
			viewH = bodyBottom() - bodyTop();
		}
		return Math.max(0, contentH - viewH);
	}

	private void openSettings(@Nullable Module m) {
		settingsModule = m;
		scroll = 0;
		listening = null;
		dragging = null;
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		layout();
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
		int accent = ThemeModule.accent();

		Render.rounded(g, left, top, left + winW, top + winH, BG);
		Render.roundedOutline(g, left, top, left + winW, top + winH, Render.alpha(accent, 0x70));

		// header
		g.text(font, "NOVA", left + 12, top + 12, accent, true);
		g.text(font, "CLIENT", left + 12 + font.width("NOVA "), top + 12, TEXT, true);
		String[] names = tabNames();
		int[][] tabs = tabBounds();
		for (int i = 0; i < names.length; i++) {
			Category c = i == 0 ? null : Category.values()[i - 1];
			boolean selected = settingsModule == null && c == category;
			boolean hover = Render.inside(mouseX, mouseY, tabs[i][0], top + 8, tabs[i][1], top + 24);
			if (selected) {
				Render.rounded(g, tabs[i][0], top + 8, tabs[i][1], top + 24, Render.alpha(accent, 0x55));
			} else if (hover) {
				Render.rounded(g, tabs[i][0], top + 8, tabs[i][1], top + 24, 0x22FFFFFF);
			}
			g.text(font, names[i], tabs[i][0] + 7, top + 12, selected ? TEXT : TEXT_DIM, false);
		}
		renderSearch(g, accent);
		g.fill(left + 8, top + HEADER, left + winW - 8, top + HEADER + 1, 0x18FFFFFF);

		// body
		if (settingsModule == null) {
			g.enableScissor(left + 1, bodyTop() - 2, left + winW - 1, bodyBottom() + 2);
			renderGrid(g, mouseX, mouseY, accent);
		} else {
			renderSettingsHeader(g, mouseX, mouseY, accent);
			g.enableScissor(left + 1, settingsListTop() - 2, left + winW - 1, bodyBottom() + 2);
			renderSettings(g, mouseX, mouseY, accent);
		}
		g.disableScissor();

		double max = maxScroll();
		if (max > 0) {
			int trackTop = settingsModule == null ? bodyTop() : settingsListTop();
			int trackH = bodyBottom() - trackTop;
			int barH = Math.max(18, (int) (trackH * trackH / (trackH + max)));
			int barY = trackTop + (int) ((trackH - barH) * (scroll / max));
			Render.rounded(g, left + winW - 6, barY, left + winW - 3, barY + barH, 0x60FFFFFF);
		}

		// footer
		int fy = top + winH - FOOTER;
		g.fill(left + 8, fy, left + winW - 8, fy + 1, 0x18FFFFFF);
		boolean hoverEditor = Render.inside(mouseX, mouseY, left + 12, fy + 5, left + 92, fy + 21);
		Render.rounded(g, left + 12, fy + 5, left + 92, fy + 21, Render.alpha(accent, hoverEditor ? 0x90 : 0x45));
		g.centeredText(font, "HUD-Editor", left + 52, fy + 9, TEXT);
		long active = NovaClient.modules().getModules().stream().filter(Module::isEnabled).count();
		String info = active + "/" + NovaClient.modules().getModules().size() + " aktiv  ·  v" + NovaClient.VERSION;
		g.text(font, info, left + winW - 12 - font.width(info), fy + 9, TEXT_MUTED, false);
	}

	private void renderSearch(GuiGraphicsExtractor g, int accent) {
		int x = searchX();
		int y = top + 8;
		Render.rounded(g, x, y, x + SEARCH_W, y + 16, 0xFF1A1A24);
		if (searchFocused) {
			Render.roundedOutline(g, x, y, x + SEARCH_W, y + 16, Render.alpha(accent, 0xC0));
		}
		// magnifier icon
		g.outline(x + 5, y + 4, 6, 6, TEXT_DIM);
		g.fill(x + 10, y + 10, x + 12, y + 12, TEXT_DIM);
		int tx = x + 16;
		if (search.isEmpty() && !searchFocused) {
			g.text(font, "Suchen…", tx, y + 4, TEXT_MUTED, false);
		} else {
			String shown = font.plainSubstrByWidth(search, SEARCH_W - 24, true);
			g.text(font, shown, tx, y + 4, TEXT, false);
			if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
				int cx = tx + font.width(shown);
				g.fill(cx + 1, y + 3, cx + 2, y + 13, TEXT);
			}
		}
	}

	private void renderGrid(GuiGraphicsExtractor g, int mouseX, int mouseY, int accent) {
		List<Module> mods = visibleModules();
		if (mods.isEmpty()) {
			g.centeredText(font, "Keine Module gefunden", left + winW / 2, bodyTop() + 40, TEXT_DIM);
			return;
		}
		int w = cardW();
		boolean mouseInBody = mouseY >= bodyTop() && mouseY < bodyBottom();
		for (int i = 0; i < mods.size(); i++) {
			Module m = mods.get(i);
			int x = cardX(i);
			int y = cardY(i);
			if (y + CARD_H < bodyTop() - 2 || y > bodyBottom() + 2) {
				continue;
			}
			boolean hover = mouseInBody && Render.inside(mouseX, mouseY, x, y, x + w, y + CARD_H);
			boolean on = m.isEnabled();
			int bg = hover ? CARD_HOVER : CARD;
			if (on) {
				bg = Render.mix(bg, accent, 0.16F);
			}
			Render.rounded(g, x, y, x + w, y + CARD_H, bg);
			if (on) {
				Render.roundedOutline(g, x, y, x + w, y + CARD_H, Render.alpha(accent, 0xB0));
			}
			g.text(font, font.plainSubstrByWidth(m.getName(), w - 16), x + 8, y + 7, TEXT, false);
			String desc = m.getDescription();
			if (font.width(desc) > w - 16) {
				desc = font.plainSubstrByWidth(desc, w - 16 - font.width("…")) + "…";
			}
			g.text(font, desc, x + 8, y + 19, TEXT_DIM, false);
			g.text(font, on ? "AN" : "AUS", x + 8, y + 33, on ? accent : TEXT_MUTED, false);
			// settings icon (three lines) and toggle
			boolean hoverGear = hover && Render.inside(mouseX, mouseY, x + w - 46, y + 29, x + w - 32, y + 43);
			int ic = hoverGear ? TEXT : TEXT_DIM;
			int gx = x + w - 43;
			g.fill(gx, y + 32, gx + 8, y + 33, ic);
			g.fill(gx, y + 35, gx + 8, y + 36, ic);
			g.fill(gx, y + 38, gx + 8, y + 39, ic);
			Render.toggle(g, x + w - 28, y + 31, on, accent);
			if (m.getKeybind() > 0) {
				String key = keyName(m.getKeybind());
				int kw = font.width(key) + 6;
				Render.rounded(g, x + w - 8 - kw, y + 5, x + w - 8, y + 16, 0xFF2A2A36);
				g.text(font, key, x + w - 5 - kw, y + 7, TEXT_DIM, false);
			}
		}
	}

	private void renderSettingsHeader(GuiGraphicsExtractor g, int mouseX, int mouseY, int accent) {
		Module m = settingsModule;
		int x = bodyX();
		int w = bodyW();
		int y = bodyTop();
		boolean hoverBack = Render.inside(mouseX, mouseY, x, y, x + 58, y + 16);
		Render.rounded(g, x, y, x + 58, y + 16, hoverBack ? 0xFF2A2A36 : 0xFF1E1E28);
		g.text(font, "< Zurück", x + 6, y + 4, TEXT, false);
		g.text(font, m.getName(), x + 68, y + 1, TEXT, true);
		int descW = w - 68 - (30 + font.width("Deaktiviert")) - 8;
		String desc = m.getDescription();
		if (font.width(desc) > descW) {
			desc = font.plainSubstrByWidth(desc, descW - font.width("…")) + "…";
		}
		g.text(font, desc, x + 68, y + 12, TEXT_DIM, false);
		g.text(font, m.isEnabled() ? "Aktiviert" : "Deaktiviert", x + w - 30 - font.width("Deaktiviert"), y + 4,
			m.isEnabled() ? accent : TEXT_MUTED, false);
		Render.toggle(g, x + w - 22, y + 3, m.isEnabled(), accent);
		g.fill(x, settingsListTop() - 6, x + w, settingsListTop() - 5, 0x18FFFFFF);
	}

	private void renderSettings(GuiGraphicsExtractor g, int mouseX, int mouseY, int accent) {
		Module m = settingsModule;
		int x = bodyX();
		int w = bodyW();
		List<Object> rows = settingRows();
		boolean mouseInList = mouseY >= settingsListTop() && mouseY < bodyBottom();
		for (int i = 0; i < rows.size(); i++) {
			int ry = rowY(i);
			if (ry + ROW_H < settingsListTop() - 2 || ry > bodyBottom() + 2) {
				continue;
			}
			boolean hover = mouseInList && Render.inside(mouseX, mouseY, x, ry, x + w, ry + ROW_H);
			Render.rounded(g, x, ry, x + w, ry + ROW_H, hover ? CARD_HOVER : CARD);
			int ty = ry + 7;
			Object row = rows.get(i);
			int right = x + w - 8;
			if (row instanceof BoolSetting b) {
				g.text(font, b.getName(), x + 10, ty, TEXT, false);
				Render.toggle(g, right - 20, ry + 6, b.isOn(), accent);
			} else if (row instanceof NumberSetting n) {
				g.text(font, n.getName(), x + 10, ty, TEXT, false);
				int sx = right - SLIDER_W;
				double t = (n.get() - n.getMin()) / (n.getMax() - n.getMin());
				int fill = sx + (int) Math.round(SLIDER_W * t);
				Render.rounded(g, sx, ry + 9, sx + SLIDER_W, ry + 13, 0xFF2E2E3A);
				if (fill - sx >= 4) {
					Render.rounded(g, sx, ry + 9, fill, ry + 13, accent);
				}
				Render.rounded(g, fill - 3, ry + 6, fill + 3, ry + 16, TEXT);
				String v = n.format();
				g.text(font, v, sx - 8 - font.width(v), ty, TEXT_DIM, false);
			} else if (row instanceof ModeSetting mode) {
				g.text(font, mode.getName(), x + 10, ty, TEXT, false);
				int pw = modePillWidth(mode);
				int px = right - pw;
				Render.rounded(g, px, ry + 4, right, ry + 18, 0xFF2A2A36);
				g.text(font, "<", px + 5, ty, TEXT_DIM, false);
				g.text(font, ">", right - 10, ty, TEXT_DIM, false);
				g.centeredText(font, mode.get(), px + pw / 2, ty, TEXT);
			} else if ("keybind".equals(row)) {
				g.text(font, "Tastenbelegung", x + 10, ty, TEXT, false);
				String label = listening == m ? "Drücke eine Taste…" : m.getKeybind() <= 0 ? "Keine" : keyName(m.getKeybind());
				int pw = font.width(label) + 14;
				Render.rounded(g, right - pw, ry + 4, right, ry + 18, listening == m ? Render.alpha(accent, 0x80) : 0xFF2A2A36);
				g.text(font, label, right - pw + 7, ty, TEXT, false);
				if (listening == m) {
					g.text(font, "ESC = entfernen", right - pw - 8 - font.width("ESC = entfernen"), ty, TEXT_MUTED, false);
				}
			} else if ("hudeditor".equals(row)) {
				g.text(font, "Position", x + 10, ty, TEXT, false);
				String label = "Im HUD-Editor verschieben";
				int pw = font.width(label) + 14;
				Render.rounded(g, right - pw, ry + 4, right, ry + 18, Render.alpha(accent, hover ? 0x90 : 0x45));
				g.text(font, label, right - pw + 7, ty, TEXT, false);
			}
		}
	}

	private int modePillWidth(ModeSetting mode) {
		int max = 0;
		for (String s : mode.getModes()) {
			max = Math.max(max, font.width(s));
		}
		return max + 30;
	}

	private static String keyName(int key) {
		return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
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

		// search box
		int sx = searchX();
		searchFocused = Render.inside(mx, my, sx, top + 8, sx + SEARCH_W, top + 24);
		if (searchFocused) {
			if (button == RIGHT) {
				search = "";
			}
			openSettings(null);
			return true;
		}

		// tabs
		int[][] tabs = tabBounds();
		for (int i = 0; i < tabs.length; i++) {
			if (Render.inside(mx, my, tabs[i][0], top + 8, tabs[i][1], top + 24)) {
				category = i == 0 ? null : Category.values()[i - 1];
				lastCategory = category;
				openSettings(null);
				return true;
			}
		}

		// footer: HUD editor
		int fy = top + winH - FOOTER;
		if (Render.inside(mx, my, left + 12, fy + 5, left + 92, fy + 21)) {
			openHudEditor();
			return true;
		}

		if (settingsModule == null) {
			if (my >= bodyTop() && my < bodyBottom() && clickGrid(mx, my, button)) {
				return true;
			}
		} else if (clickSettings(mx, my, button)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void openHudEditor() {
		NovaClient.config().save();
		minecraft.gui.setScreen(new HudEditorScreen(this));
	}

	private boolean clickGrid(double mx, double my, int button) {
		List<Module> mods = visibleModules();
		int w = cardW();
		for (int i = 0; i < mods.size(); i++) {
			int x = cardX(i);
			int y = cardY(i);
			if (!Render.inside(mx, my, x, y, x + w, y + CARD_H)) {
				continue;
			}
			Module m = mods.get(i);
			boolean onGear = Render.inside(mx, my, x + w - 46, y + 29, x + w - 32, y + 43);
			if (button == RIGHT || (button == LEFT && onGear)) {
				openSettings(m);
			} else if (button == LEFT) {
				m.toggle();
			}
			return true;
		}
		return false;
	}

	private boolean clickSettings(double mx, double my, int button) {
		Module m = settingsModule;
		int x = bodyX();
		int w = bodyW();
		int y = bodyTop();
		if (Render.inside(mx, my, x, y, x + 58, y + 16)) {
			openSettings(null);
			return true;
		}
		if (Render.inside(mx, my, x + w - 30 - font.width("Deaktiviert"), y, x + w, y + 16)) {
			m.toggle();
			return true;
		}
		if (my < settingsListTop() || my >= bodyBottom()) {
			return false;
		}
		List<Object> rows = settingRows();
		for (int i = 0; i < rows.size(); i++) {
			int ry = rowY(i);
			if (!Render.inside(mx, my, x, ry, x + w, ry + ROW_H)) {
				continue;
			}
			Object row = rows.get(i);
			if (row instanceof BoolSetting b) {
				b.toggle();
			} else if (row instanceof NumberSetting n) {
				dragging = n;
				dragSliderX = x + w - 8 - SLIDER_W;
				updateSlider(mx);
			} else if (row instanceof ModeSetting mode) {
				mode.cycle(button == RIGHT ? -1 : 1);
			} else if ("keybind".equals(row)) {
				listening = m;
			} else if ("hudeditor".equals(row)) {
				openHudEditor();
			}
			return true;
		}
		return false;
	}

	private void updateSlider(double mx) {
		if (dragging == null) {
			return;
		}
		double t = Math.max(0, Math.min(1, (mx - dragSliderX) / SLIDER_W));
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
		scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 20));
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (listening != null || !event.isAllowedChatCharacter()) {
			return false;
		}
		if (search.length() < 32) {
			search += event.codepointAsString();
			searchFocused = true;
			scroll = 0;
			settingsModule = null;
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (listening != null) {
			if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
				listening.setKeybind(0);
			} else {
				listening.setKeybind(key);
			}
			listening = null;
			return true;
		}
		if (key == InputConstants.KEY_BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.offsetByCodePoints(search.length(), -1));
			scroll = 0;
			return true;
		}
		if (key == InputConstants.KEY_ESCAPE) {
			if (settingsModule != null) {
				openSettings(null);
				return true;
			}
			if (!search.isEmpty() || searchFocused) {
				search = "";
				searchFocused = false;
				return true;
			}
		}
		if (key == InputConstants.KEY_RSHIFT) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	// ------------------------------------------------------------------ positions (used by the automated tests)

	/** Centre of the card body of a module on the grid page, or null if not visible. */
	public int @Nullable [] cardCenter(Module m) {
		layout();
		int i = visibleModules().indexOf(m);
		return i < 0 ? null : new int[] {cardX(i) + 30, cardY(i) + 12};
	}

	/** Centre of the settings (menu) icon of a card. */
	public int @Nullable [] gearCenter(Module m) {
		layout();
		int i = visibleModules().indexOf(m);
		return i < 0 ? null : new int[] {cardX(i) + cardW() - 39, cardY(i) + 35};
	}

	/** Centre of tab i (0 = "Alle"). */
	public int[] tabCenter(int i) {
		layout();
		int[][] tabs = tabBounds();
		return new int[] {(tabs[i][0] + tabs[i][1]) / 2, top + 16};
	}

	/** Centre of settings row i of the open settings page; for sliders pass the wanted fraction. */
	public int[] rowPoint(int i, double sliderFraction) {
		layout();
		int x = sliderFraction >= 0 ? bodyX() + bodyW() - 8 - SLIDER_W + (int) Math.round(SLIDER_W * sliderFraction) : bodyX() + 40;
		return new int[] {x, rowY(i) + ROW_H / 2};
	}

	public int[] backButtonCenter() {
		layout();
		return new int[] {bodyX() + 29, bodyTop() + 8};
	}

	public int[] hudEditorButtonCenter() {
		layout();
		return new int[] {left + 52, top + winH - FOOTER + 13};
	}

	public @Nullable Module getSettingsModule() {
		return settingsModule;
	}

	public String getSearch() {
		return search;
	}
}
