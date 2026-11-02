package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.config.ConfigManager;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.KeySetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.setting.Setting;
import dev.novaclient.util.KeyState;
import dev.novaclient.util.Notifications;
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
 * Grid: click a card to toggle the module, right click or the menu icon opens its settings.
 * Footer: HUD editor, profiles and design (accent colour) pages.
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
	private static final int SWATCH = 18;

	private static final int CARD = 0xFF181820;
	private static final int CARD_HOVER = 0xFF20202B;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_DIM = 0xFF8A8A9A;
	private static final int TEXT_MUTED = 0xFF5E5E6E;
	private static final int RED = 0xFFE05555;

	public enum Page { GRID, SETTINGS, PROFILES, DESIGN }

	/** A profile entry on the profiles page (wrapped so names never clash with row ids). */
	public record ProfileRow(String name) {
	}

	private static @Nullable Category lastCategory;

	private final @Nullable Screen parent;
	private Page page = Page.GRID;
	private @Nullable Category category = lastCategory;
	private @Nullable Module settingsModule;
	/** Module whose keybind is being recorded, or the key setting being recorded. */
	private @Nullable Module listening;
	private @Nullable KeySetting listeningKey;
	private @Nullable NumberSetting dragging;
	private int dragSliderX;
	private String search = "";
	private boolean searchFocused;
	private String newProfileName = "";
	private boolean nameFocused;
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
		if (openSettings != null) {
			this.settingsModule = openSettings;
			this.page = Page.SETTINGS;
		}
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

	private String logo() {
		return winW >= 440 ? "NOVA CLIENT" : "NOVA";
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

	private int[][] tabBounds() {
		String[] names = tabNames();
		int[][] out = new int[names.length][2];
		int x = left + 12 + font.width(logo()) + 12;
		for (int i = 0; i < names.length; i++) {
			int w = font.width(names[i]) + 12;
			out[i][0] = x;
			out[i][1] = x + w;
			x += w + 3;
		}
		return out;
	}

	private int searchW() {
		int[][] tabs = tabBounds();
		return Math.max(60, Math.min(120, left + winW - 12 - tabs[tabs.length - 1][1] - 8));
	}

	private int searchX() {
		return left + winW - 12 - searchW();
	}

	private static boolean matches(Module m, String q) {
		if (q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
			return true;
		}
		for (Setting<?> s : m.getSettings()) {
			if (s.getName().toLowerCase(Locale.ROOT).contains(q)) {
				return true;
			}
		}
		return false;
	}

	private List<Module> visibleModules() {
		String q = search.trim().toLowerCase(Locale.ROOT);
		List<Module> out = new ArrayList<>();
		for (Module m : NovaClient.modules().getModules()) {
			if ((category == null || m.getCategory() == category || !q.isEmpty()) && matches(m, q)) {
				out.add(m);
			}
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

	/** Rows of the current list page: settings, keybind and buttons. */
	private List<Object> rows() {
		List<Object> rows = new ArrayList<>();
		if (page == Page.SETTINGS && settingsModule != null) {
			rows.addAll(settingsModule.getSettings());
			rows.add("keybind");
			if (settingsModule instanceof HudModule) {
				rows.add("hudeditor");
				rows.add("resetpos");
			}
			rows.add("reset");
		} else if (page == Page.DESIGN) {
			rows.addAll(ThemeModule.get().getSettings());
		} else if (page == Page.PROFILES) {
			for (String name : NovaClient.config().listProfiles()) {
				rows.add(new ProfileRow(name));
			}
			rows.add("newprofile");
			rows.add("presets");
		}
		return rows;
	}

	private int swatchRows() {
		int perRow = Math.max(1, (bodyW() + 6) / (SWATCH + 6));
		return (ThemeModule.PRESETS.length + perRow - 1) / perRow;
	}

	private int listTop() {
		if (page == Page.DESIGN) {
			return bodyTop() + 22 + swatchRows() * (SWATCH + 6) + 8;
		}
		return bodyTop() + (page == Page.PROFILES ? 22 : 40);
	}

	private int rowY(int index) {
		return listTop() + index * (ROW_H + ROW_GAP) - (int) scroll;
	}

	private double maxScroll() {
		int contentH;
		int viewH;
		if (page == Page.GRID) {
			int r = (visibleModules().size() + columns() - 1) / columns();
			contentH = r * (CARD_H + CARD_GAP);
			viewH = bodyBottom() - bodyTop();
		} else {
			contentH = rows().size() * (ROW_H + ROW_GAP);
			viewH = bodyBottom() - listTop();
		}
		return Math.max(0, contentH - viewH);
	}

	private void open(Page p, @Nullable Module m) {
		page = p;
		settingsModule = m;
		scroll = 0;
		listening = null;
		listeningKey = null;
		dragging = null;
		nameFocused = false;
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		layout();
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
		int accent = ThemeModule.accent();
		int bg = Render.alpha(0xFF0E0E14, ThemeModule.menuAlpha());

		Render.rounded(g, left, top, left + winW, top + winH, bg);
		Render.roundedOutline(g, left, top, left + winW, top + winH, Render.alpha(accent, 0x70));

		// header
		String logo = logo();
		g.text(font, "NOVA", left + 12, top + 12, accent, true);
		if (logo.length() > 4) {
			g.text(font, "CLIENT", left + 12 + font.width("NOVA "), top + 12, TEXT, true);
		}
		String[] names = tabNames();
		int[][] tabs = tabBounds();
		for (int i = 0; i < names.length; i++) {
			Category c = i == 0 ? null : Category.values()[i - 1];
			boolean selected = page == Page.GRID && c == category && search.isEmpty();
			boolean hover = Render.inside(mouseX, mouseY, tabs[i][0], top + 8, tabs[i][1], top + 24);
			if (selected) {
				Render.rounded(g, tabs[i][0], top + 8, tabs[i][1], top + 24, Render.alpha(accent, 0x55));
			} else if (hover) {
				Render.rounded(g, tabs[i][0], top + 8, tabs[i][1], top + 24, 0x22FFFFFF);
			}
			g.text(font, names[i], tabs[i][0] + 6, top + 12, selected ? TEXT : TEXT_DIM, false);
		}
		renderSearch(g, accent);
		g.fill(left + 8, top + HEADER, left + winW - 8, top + HEADER + 1, 0x18FFFFFF);

		// body
		switch (page) {
			case GRID -> {
				g.enableScissor(left + 1, bodyTop() - 2, left + winW - 1, bodyBottom() + 2);
				renderGrid(g, mouseX, mouseY, accent);
				g.disableScissor();
			}
			case SETTINGS -> renderSettingsHeader(g, mouseX, mouseY, accent);
			case DESIGN -> renderDesignHeader(g, mouseX, mouseY, accent);
			case PROFILES -> renderPageTitle(g, mouseX, mouseY, "Profile",
				"Aktiv: " + NovaClient.config().getActiveProfile() + "  ·  Einstellungen werden pro Profil gespeichert");
		}
		if (page != Page.GRID) {
			g.enableScissor(left + 1, listTop() - 2, left + winW - 1, bodyBottom() + 2);
			renderRows(g, mouseX, mouseY, accent);
			g.disableScissor();
		}

		double max = maxScroll();
		if (max > 0) {
			int trackTop = page == Page.GRID ? bodyTop() : listTop();
			int trackH = bodyBottom() - trackTop;
			int barH = Math.max(18, (int) (trackH * trackH / (trackH + max)));
			int barY = trackTop + (int) ((trackH - barH) * (scroll / max));
			Render.rounded(g, left + winW - 6, barY, left + winW - 3, barY + barH, 0x60FFFFFF);
		}

		// footer
		int fy = top + winH - FOOTER;
		g.fill(left + 8, fy, left + winW - 8, fy + 1, 0x18FFFFFF);
		String[] buttons = footerButtons();
		for (int i = 0; i < buttons.length; i++) {
			int[] b = footerButton(i);
			boolean hover = Render.inside(mouseX, mouseY, b[0], b[1], b[2], b[3]);
			boolean current = i == 1 && page == Page.PROFILES || i == 2 && page == Page.DESIGN;
			Render.rounded(g, b[0], b[1], b[2], b[3], Render.alpha(accent, hover || current ? 0x90 : 0x40));
			g.centeredText(font, buttons[i], (b[0] + b[2]) / 2, b[1] + 4, TEXT);
		}
		long active = NovaClient.modules().getModules().stream().filter(Module::isEnabled).count();
		String info = active + "/" + NovaClient.modules().getModules().size() + " aktiv · v" + NovaClient.VERSION;
		int infoX = left + winW - 12 - font.width(info);
		if (infoX > footerButton(buttons.length - 1)[2] + 6) {
			g.text(font, info, infoX, fy + 9, TEXT_MUTED, false);
		}
	}

	private static String[] footerButtons() {
		return new String[] {"HUD-Editor", "Profile", "Design"};
	}

	private int[] footerButton(int i) {
		int fy = top + winH - FOOTER;
		int x = left + 12;
		String[] b = footerButtons();
		for (int j = 0; j < i; j++) {
			x += font.width(b[j]) + 16 + 4;
		}
		return new int[] {x, fy + 5, x + font.width(b[i]) + 16, fy + 21};
	}

	private void renderSearch(GuiGraphicsExtractor g, int accent) {
		int x = searchX();
		int w = searchW();
		int y = top + 8;
		Render.rounded(g, x, y, x + w, y + 16, 0xFF1A1A24);
		if (searchFocused) {
			Render.roundedOutline(g, x, y, x + w, y + 16, Render.alpha(accent, 0xC0));
		}
		g.outline(x + 5, y + 4, 6, 6, TEXT_DIM);
		g.fill(x + 10, y + 10, x + 12, y + 12, TEXT_DIM);
		int tx = x + 16;
		if (search.isEmpty() && !searchFocused) {
			g.text(font, "Suchen…", tx, y + 4, TEXT_MUTED, false);
		} else {
			String shown = font.plainSubstrByWidth(search, w - 24, true);
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
			g.text(font, ellipsis(m.getDescription(), w - 16), x + 8, y + 19, TEXT_DIM, false);
			g.text(font, on ? "AN" : "AUS", x + 8, y + 33, on ? accent : TEXT_MUTED, false);
			if (!search.isEmpty() && category == null) {
				String cat = m.getCategory().getDisplayName();
				g.text(font, cat, x + 8 + font.width("AUS") + 8, y + 33, TEXT_MUTED, false);
			}
			boolean hoverGear = hover && Render.inside(mouseX, mouseY, x + w - 46, y + 29, x + w - 32, y + 43);
			int ic = hoverGear ? TEXT : TEXT_DIM;
			int gx = x + w - 43;
			g.fill(gx, y + 32, gx + 8, y + 33, ic);
			g.fill(gx, y + 35, gx + 8, y + 36, ic);
			g.fill(gx, y + 38, gx + 8, y + 39, ic);
			Render.toggle(g, x + w - 28, y + 31, on, accent);
			if (m.getKeybind() > 0) {
				String key = KeyState.name(m.getKeybind());
				int kw = font.width(key) + 6;
				Render.rounded(g, x + w - 8 - kw, y + 5, x + w - 8, y + 16, 0xFF2A2A36);
				g.text(font, key, x + w - 5 - kw, y + 7, TEXT_DIM, false);
			}
		}
	}

	private String ellipsis(String s, int maxW) {
		if (font.width(s) <= maxW) {
			return s;
		}
		return font.plainSubstrByWidth(s, maxW - font.width("…")) + "…";
	}

	private void renderBackButton(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = bodyX();
		int y = bodyTop();
		boolean hoverBack = Render.inside(mouseX, mouseY, x, y, x + 58, y + 16);
		Render.rounded(g, x, y, x + 58, y + 16, hoverBack ? 0xFF2A2A36 : 0xFF1E1E28);
		g.text(font, "< Zurück", x + 6, y + 4, TEXT, false);
	}

	private void renderPageTitle(GuiGraphicsExtractor g, int mouseX, int mouseY, String title, String subtitle) {
		renderBackButton(g, mouseX, mouseY);
		int x = bodyX() + 68;
		g.text(font, title, x, bodyTop() + 1, TEXT, true);
		g.text(font, ellipsis(subtitle, bodyW() - 76), x, bodyTop() + 11, TEXT_DIM, false);
	}

	private void renderSettingsHeader(GuiGraphicsExtractor g, int mouseX, int mouseY, int accent) {
		Module m = settingsModule;
		int x = bodyX();
		int w = bodyW();
		int y = bodyTop();
		renderBackButton(g, mouseX, mouseY);
		g.text(font, m.getName(), x + 68, y + 1, TEXT, true);
		int descW = w - 68 - (30 + font.width("Deaktiviert")) - 8;
		g.text(font, ellipsis(m.getDescription(), descW), x + 68, y + 12, TEXT_DIM, false);
		g.text(font, m.isEnabled() ? "Aktiviert" : "Deaktiviert", x + w - 30 - font.width("Deaktiviert"), y + 4,
			m.isEnabled() ? accent : TEXT_MUTED, false);
		Render.toggle(g, x + w - 22, y + 3, m.isEnabled(), accent);
		g.fill(x, listTop() - 6, x + w, listTop() - 5, 0x18FFFFFF);
	}

	private int[] swatchPos(int i) {
		int perRow = Math.max(1, (bodyW() + 6) / (SWATCH + 6));
		int x = bodyX() + (i % perRow) * (SWATCH + 6);
		int y = bodyTop() + 22 + (i / perRow) * (SWATCH + 6);
		return new int[] {x, y};
	}

	private void renderDesignHeader(GuiGraphicsExtractor g, int mouseX, int mouseY, int accent) {
		renderPageTitle(g, mouseX, mouseY, "Design", "Akzentfarbe anklicken oder unten genau einstellen");
		ThemeModule theme = ThemeModule.get();
		for (int i = 0; i < ThemeModule.PRESETS.length; i++) {
			ThemeModule.Preset p = ThemeModule.PRESETS[i];
			int[] pos = swatchPos(i);
			boolean selected = theme.matches(p);
			boolean hover = Render.inside(mouseX, mouseY, pos[0], pos[1], pos[0] + SWATCH, pos[1] + SWATCH);
			if (selected || hover) {
				Render.rounded(g, pos[0] - 2, pos[1] - 2, pos[0] + SWATCH + 2, pos[1] + SWATCH + 2, selected ? TEXT : 0x80FFFFFF);
			}
			Render.rounded(g, pos[0], pos[1], pos[0] + SWATCH, pos[1] + SWATCH, p.color());
			if (hover) {
				g.setTooltipForNextFrame(font, Component.literal(p.name()), mouseX, mouseY);
			}
		}
		g.fill(bodyX(), listTop() - 6, bodyX() + bodyW(), listTop() - 5, 0x18FFFFFF);
	}

	private void renderRows(GuiGraphicsExtractor g, int mouseX, int mouseY, int accent) {
		int x = bodyX();
		int w = bodyW();
		List<Object> rows = rows();
		boolean mouseInList = mouseY >= listTop() && mouseY < bodyBottom();
		for (int i = 0; i < rows.size(); i++) {
			int ry = rowY(i);
			if (ry + ROW_H < listTop() - 2 || ry > bodyBottom() + 2) {
				continue;
			}
			boolean hover = mouseInList && Render.inside(mouseX, mouseY, x, ry, x + w, ry + ROW_H);
			Object row = rows.get(i);
			Render.rounded(g, x, ry, x + w, ry + ROW_H, hover ? CARD_HOVER : CARD);
			int ty = ry + 7;
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
			} else if (row instanceof KeySetting k) {
				g.text(font, k.getName(), x + 10, ty, TEXT, false);
				keyPill(g, right, ry, ty, listeningKey == k, k.display(), accent);
			} else if ("keybind".equals(row)) {
				g.text(font, "An/Aus-Taste", x + 10, ty, TEXT, false);
				Module m = settingsModule;
				keyPill(g, right, ry, ty, listening == m, m.getKeybind() <= 0 ? "Keine" : KeyState.name(m.getKeybind()), accent);
			} else if ("hudeditor".equals(row)) {
				g.text(font, "Position", x + 10, ty, TEXT, false);
				button(g, right, ry, ty, "Im HUD-Editor verschieben", Render.alpha(accent, hover ? 0x90 : 0x45));
			} else if ("resetpos".equals(row)) {
				g.text(font, "Standardposition", x + 10, ty, TEXT, false);
				button(g, right, ry, ty, "Zurücksetzen", hover ? 0xFF34343F : 0xFF2A2A36);
			} else if ("reset".equals(row)) {
				g.text(font, "Alle Einstellungen", x + 10, ty, TEXT, false);
				button(g, right, ry, ty, "Auf Standard zurücksetzen", hover ? 0xFF5A2A2A : 0xFF3A2228);
			} else if ("newprofile".equals(row)) {
				g.text(font, "Neues Profil:", x + 10, ty, TEXT, false);
				int fx = x + 16 + font.width("Neues Profil:");
				int bw = font.width("Erstellen") + 14;
				int fw = right - bw - 6 - fx;
				Render.rounded(g, fx, ry + 4, fx + fw, ry + 18, 0xFF101016);
				if (nameFocused) {
					Render.roundedOutline(g, fx, ry + 4, fx + fw, ry + 18, Render.alpha(accent, 0xC0));
				}
				String shown = newProfileName.isEmpty() && !nameFocused ? "Name eingeben…" : newProfileName;
				g.text(font, font.plainSubstrByWidth(shown, fw - 10, true), fx + 5, ty, newProfileName.isEmpty() && !nameFocused ? TEXT_MUTED : TEXT, false);
				if (nameFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
					int cx = fx + 5 + font.width(font.plainSubstrByWidth(newProfileName, fw - 10, true));
					g.fill(cx + 1, ry + 6, cx + 2, ry + 16, TEXT);
				}
				button(g, right, ry, ty, "Erstellen", Render.alpha(accent, 0x70));
			} else if ("presets".equals(row)) {
				g.text(font, "Vorlage:", x + 10, ty, TEXT, false);
				int bx = right;
				for (int p = ConfigManager.PRESETS.length - 1; p >= 0; p--) {
					String label = ConfigManager.PRESETS[p];
					int bw = font.width(label) + 14;
					boolean hb = Render.inside(mouseX, mouseY, bx - bw, ry + 4, bx, ry + 18);
					Render.rounded(g, bx - bw, ry + 4, bx, ry + 18, Render.alpha(accent, hb ? 0x90 : 0x45));
					g.text(font, label, bx - bw + 7, ty, TEXT, false);
					bx -= bw + 4;
				}
			} else if (row instanceof ProfileRow pr) {
				String profile = pr.name();
				boolean isActive = profile.equals(NovaClient.config().getActiveProfile());
				g.text(font, profile, x + 10, ty, isActive ? accent : TEXT, false);
				int dw = font.width("Löschen") + 14;
				if (!isActive) {
					boolean hd = Render.inside(mouseX, mouseY, right - dw, ry + 4, right, ry + 18);
					Render.rounded(g, right - dw, ry + 4, right, ry + 18, hd ? RED : 0xFF3A2228);
					g.text(font, "Löschen", right - dw + 7, ty, TEXT, false);
				}
				String label = isActive ? "Aktiv" : "Laden";
				int lw = font.width(label) + 14;
				int lx = right - dw - 4 - lw;
				boolean hl = !isActive && Render.inside(mouseX, mouseY, lx, ry + 4, lx + lw, ry + 18);
				Render.rounded(g, lx, ry + 4, lx + lw, ry + 18, isActive ? Render.alpha(accent, 0xA0) : Render.alpha(accent, hl ? 0x90 : 0x45));
				g.text(font, label, lx + 7, ty, TEXT, false);
			}
		}
	}

	private void keyPill(GuiGraphicsExtractor g, int right, int ry, int ty, boolean recording, String label, int accent) {
		String text = recording ? "Taste drücken…" : label;
		int pw = font.width(text) + 14;
		Render.rounded(g, right - pw, ry + 4, right, ry + 18, recording ? Render.alpha(accent, 0x80) : 0xFF2A2A36);
		g.text(font, text, right - pw + 7, ty, TEXT, false);
		if (recording) {
			String hint = "ESC = keine";
			g.text(font, hint, right - pw - 8 - font.width(hint), ty, TEXT_MUTED, false);
		}
	}

	private void button(GuiGraphicsExtractor g, int right, int ry, int ty, String label, int color) {
		int pw = font.width(label) + 14;
		Render.rounded(g, right - pw, ry + 4, right, ry + 18, color);
		g.text(font, label, right - pw + 7, ty, TEXT, false);
	}

	private int modePillWidth(ModeSetting mode) {
		int max = 0;
		for (String s : mode.getModes()) {
			max = Math.max(max, font.width(s));
		}
		return max + 30;
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		layout();
		double mx = event.x();
		double my = event.y();
		int button = event.button();

		if (listeningKey != null) {
			// mouse buttons other than left/right can be bound (e.g. side buttons)
			if (button != LEFT && button != RIGHT) {
				listeningKey.set(KeyState.MOUSE_OFFSET + button);
			}
			listeningKey = null;
			return true;
		}
		if (listening != null) {
			listening = null;
			return true;
		}

		int sx = searchX();
		searchFocused = Render.inside(mx, my, sx, top + 8, sx + searchW(), top + 24);
		if (searchFocused) {
			if (button == RIGHT) {
				search = "";
			}
			open(Page.GRID, null);
			searchFocused = true;
			return true;
		}

		int[][] tabs = tabBounds();
		for (int i = 0; i < tabs.length; i++) {
			if (Render.inside(mx, my, tabs[i][0], top + 8, tabs[i][1], top + 24)) {
				category = i == 0 ? null : Category.values()[i - 1];
				lastCategory = category;
				search = "";
				open(Page.GRID, null);
				return true;
			}
		}

		String[] buttons = footerButtons();
		for (int i = 0; i < buttons.length; i++) {
			int[] b = footerButton(i);
			if (Render.inside(mx, my, b[0], b[1], b[2], b[3])) {
				switch (i) {
					case 0 -> openHudEditor();
					case 1 -> open(page == Page.PROFILES ? Page.GRID : Page.PROFILES, null);
					default -> open(page == Page.DESIGN ? Page.GRID : Page.DESIGN, null);
				}
				return true;
			}
		}

		nameFocused = false;
		boolean handled = switch (page) {
			case GRID -> my >= bodyTop() && my < bodyBottom() && clickGrid(mx, my, button);
			case SETTINGS, PROFILES, DESIGN -> clickPage(mx, my, button);
		};
		return handled || super.mouseClicked(event, doubleClick);
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
				open(Page.SETTINGS, m);
			} else if (button == LEFT) {
				m.toggle();
			}
			return true;
		}
		return false;
	}

	private boolean clickPage(double mx, double my, int button) {
		int x = bodyX();
		int w = bodyW();
		int y = bodyTop();
		if (Render.inside(mx, my, x, y, x + 58, y + 16)) {
			open(Page.GRID, null);
			return true;
		}
		if (page == Page.SETTINGS && Render.inside(mx, my, x + w - 30 - font.width("Deaktiviert"), y, x + w, y + 16)) {
			settingsModule.toggle();
			return true;
		}
		if (page == Page.DESIGN) {
			for (int i = 0; i < ThemeModule.PRESETS.length; i++) {
				int[] pos = swatchPos(i);
				if (Render.inside(mx, my, pos[0], pos[1], pos[0] + SWATCH, pos[1] + SWATCH)) {
					ThemeModule.get().apply(ThemeModule.PRESETS[i]);
					ThemeModule.get().setEnabled(true);
					return true;
				}
			}
		}
		if (my < listTop() || my >= bodyBottom()) {
			return false;
		}
		List<Object> rows = rows();
		int right = x + w - 8;
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
				dragSliderX = right - SLIDER_W;
				updateSlider(mx);
			} else if (row instanceof ModeSetting mode) {
				mode.cycle(button == RIGHT ? -1 : 1);
			} else if (row instanceof KeySetting k) {
				listeningKey = k;
			} else if ("keybind".equals(row)) {
				listening = settingsModule;
			} else if ("hudeditor".equals(row)) {
				openHudEditor();
			} else if ("resetpos".equals(row)) {
				((HudModule) settingsModule).resetPosition();
			} else if ("reset".equals(row)) {
				settingsModule.resetToDefaults();
				Notifications.push(settingsModule.getName() + " zurückgesetzt", 0);
			} else if ("newprofile".equals(row)) {
				int bw = font.width("Erstellen") + 14;
				if (mx >= right - bw) {
					createProfile();
				} else {
					nameFocused = true;
				}
			} else if ("presets".equals(row)) {
				int bx = right;
				for (int p = ConfigManager.PRESETS.length - 1; p >= 0; p--) {
					int bw = font.width(ConfigManager.PRESETS[p]) + 14;
					if (mx >= bx - bw && mx < bx) {
						String name = NovaClient.config().createFromPreset(ConfigManager.PRESETS[p]);
						Notifications.push("Profil \"" + name + "\" erstellt", 0);
						break;
					}
					bx -= bw + 4;
				}
			} else if (row instanceof ProfileRow pr) {
				String profile = pr.name();
				boolean isActive = profile.equals(NovaClient.config().getActiveProfile());
				int dw = font.width("Löschen") + 14;
				int lw = font.width(isActive ? "Aktiv" : "Laden") + 14;
				int lx = right - dw - 4 - lw;
				if (!isActive && mx >= right - dw) {
					NovaClient.config().delete(profile);
					Notifications.push("Profil \"" + profile + "\" gelöscht", 0xFF777788);
				} else if (!isActive && mx >= lx && mx < lx + lw) {
					NovaClient.config().switchTo(profile);
					Notifications.push("Profil \"" + profile + "\" geladen", 0);
				}
			}
			return true;
		}
		return false;
	}

	private void createProfile() {
		String name = NovaClient.config().createCopy(newProfileName.isBlank() ? "Profil" : newProfileName);
		newProfileName = "";
		nameFocused = false;
		Notifications.push("Profil \"" + name + "\" erstellt", 0);
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
		if (listening != null || listeningKey != null || !event.isAllowedChatCharacter()) {
			return false;
		}
		if (nameFocused) {
			if (newProfileName.length() < 24) {
				newProfileName += event.codepointAsString();
			}
			return true;
		}
		if (search.length() < 32) {
			search += event.codepointAsString();
			searchFocused = true;
			if (page != Page.GRID) {
				open(Page.GRID, null);
				searchFocused = true;
			}
			scroll = 0;
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (listeningKey != null) {
			boolean clear = key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE;
			listeningKey.set(clear ? 0 : key);
			listeningKey = null;
			return true;
		}
		if (listening != null) {
			boolean clear = key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE;
			listening.setKeybind(clear ? 0 : key);
			listening = null;
			return true;
		}
		if (nameFocused) {
			if (key == InputConstants.KEY_BACKSPACE && !newProfileName.isEmpty()) {
				newProfileName = newProfileName.substring(0, newProfileName.offsetByCodePoints(newProfileName.length(), -1));
				return true;
			}
			if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
				createProfile();
				return true;
			}
			if (key == InputConstants.KEY_ESCAPE) {
				nameFocused = false;
				return true;
			}
		}
		if (key == InputConstants.KEY_BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.offsetByCodePoints(search.length(), -1));
			scroll = 0;
			return true;
		}
		if (key == InputConstants.KEY_ESCAPE) {
			if (page != Page.GRID) {
				open(Page.GRID, null);
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

	public int @Nullable [] cardCenter(Module m) {
		layout();
		int i = visibleModules().indexOf(m);
		return i < 0 ? null : new int[] {cardX(i) + 30, cardY(i) + 12};
	}

	public int @Nullable [] gearCenter(Module m) {
		layout();
		int i = visibleModules().indexOf(m);
		return i < 0 ? null : new int[] {cardX(i) + cardW() - 39, cardY(i) + 35};
	}

	public int[] tabCenter(int i) {
		layout();
		int[][] tabs = tabBounds();
		return new int[] {(tabs[i][0] + tabs[i][1]) / 2, top + 16};
	}

	/** Point in row i; xFromRight >= 0 picks a point that many pixels left of the row's right edge, sliderFraction >= 0 a slider position. */
	public int[] rowPoint(int i, double sliderFraction) {
		layout();
		int x = sliderFraction >= 0 ? bodyX() + bodyW() - 8 - SLIDER_W + (int) Math.round(SLIDER_W * sliderFraction) : bodyX() + 40;
		return new int[] {x, rowY(i) + ROW_H / 2};
	}

	public int[] rowPointFromRight(int i, int xFromRight) {
		layout();
		return new int[] {bodyX() + bodyW() - 8 - xFromRight, rowY(i) + ROW_H / 2};
	}

	public int rowCount() {
		return rows().size();
	}

	public int rowIndexOf(Object row) {
		return rows().indexOf(row);
	}

	public int[] swatchCenter(int i) {
		layout();
		int[] p = swatchPos(i);
		return new int[] {p[0] + SWATCH / 2, p[1] + SWATCH / 2};
	}

	public int[] backButtonCenter() {
		layout();
		return new int[] {bodyX() + 29, bodyTop() + 8};
	}

	public int[] footerButtonCenter(int i) {
		layout();
		int[] b = footerButton(i);
		return new int[] {(b[0] + b[2]) / 2, (b[1] + b[3]) / 2};
	}

	public int[] hudEditorButtonCenter() {
		return footerButtonCenter(0);
	}

	public Page getPage() {
		return page;
	}

	public @Nullable Module getSettingsModule() {
		return settingsModule;
	}

	public String getSearch() {
		return search;
	}
}
