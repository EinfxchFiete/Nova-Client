package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.friends.Friends;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.util.Render;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** The start menu on the menu key: Module, Cosmetics, Friends or Music (keys 1 to 4). */
public class NovaHubScreen extends Screen {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final String[] TITLES = {"Module", "Cosmetics", "Freunde", "Musik"};
	private static final String[] ICONS = {"☰", "✦", "❤", "♪"};
	private static final int CARDS = TITLES.length;

	public NovaHubScreen() {
		super(Component.literal("Nova Client"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int cardW() {
		return Math.min(120, (width - 70) / CARDS);
	}

	private int cardH() {
		return Math.min(120, height - 110);
	}

	/** {x0, y0, x1, y1} of card i. */
	private int[] card(int i) {
		int w = cardW();
		int gap = 12;
		int total = w * CARDS + gap * (CARDS - 1);
		int x0 = (width - total) / 2 + i * (w + gap);
		int y0 = height / 2 - cardH() / 2 + 8;
		return new int[] {x0, y0, x0 + w, y0 + cardH()};
	}

	private int[] hudButton() {
		int w = font.width("HUD-Editor") + 16;
		int y = card(0)[3] + 12;
		return new int[] {width / 2 - w / 2, y, width / 2 + w / 2, y + 16};
	}

	private String subtitle(int i) {
		return switch (i) {
			case 0 -> NovaClient.modules().getModules().size() + " Module, HUD, Profile";
			case 1 -> "Capes, Kopf, Flügel …";
			case 3 -> {
				dev.novaclient.music.NowPlaying.Song s = dev.novaclient.music.NowPlaying.current();
				yield s != null ? "♪ " + s.title() : "Songtext, Verlauf, Top";
			}
			default -> {
				int online = Friends.onlineCount();
				int req = Friends.requests().size();
				yield online + " online" + (req > 0 ? " · " + req + " Anfrage" + (req == 1 ? "" : "n") : "");
			}
		};
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		int accent = ThemeModule.accent();
		int[] c0 = card(0);
		g.centeredText(font, Component.literal("NOVA CLIENT"), width / 2, c0[1] - 30, accent);
		g.centeredText(font, Component.literal("Was möchtest du öffnen?"), width / 2, c0[1] - 18, 0xFFB0B0C0);
		for (int i = 0; i < CARDS; i++) {
			int[] c = card(i);
			boolean hover = Render.inside(mouseX, mouseY, c[0], c[1], c[2], c[3]);
			int bg = hover ? Render.mix(0xFF16161E, accent, 0.25F) : Render.alpha(0xFF101016, ThemeModule.menuAlpha());
			Render.rounded(g, c[0], c[1], c[2], c[3], bg);
			Render.roundedOutline(g, c[0], c[1], c[2], c[3], hover ? accent : Render.alpha(accent, 0x60));
			int cx = (c[0] + c[2]) / 2;
			int iy = c[1] + (c[3] - c[1]) / 2 - 26;
			g.pose().pushMatrix();
			g.pose().translate(cx, iy);
			g.pose().scale(3.0F, 3.0F);
			g.centeredText(font, Component.literal(ICONS[i]), 0, -4, hover ? 0xFFFFFFFF : accent);
			g.pose().popMatrix();
			g.centeredText(font, Component.literal(TITLES[i]), cx, c[3] - 34, 0xFFFFFFFF);
			String sub = font.plainSubstrByWidth(subtitle(i), c[2] - c[0] - 8);
			g.centeredText(font, Component.literal(sub), cx, c[3] - 20, 0xFF8E8EA4);
			g.text(font, String.valueOf(i + 1), c[0] + 6, c[1] + 5, 0xFF55556A, false);
			if (i == 2 && !Friends.requests().isEmpty()) {
				Render.rounded(g, c[2] - 16, c[1] + 4, c[2] - 4, c[1] + 16, 0xFFE53935);
				g.centeredText(font, Component.literal(String.valueOf(Friends.requests().size())), c[2] - 10, c[1] + 6, 0xFFFFFFFF);
			}
		}
		int[] hb = hudButton();
		boolean hoverHud = Render.inside(mouseX, mouseY, hb[0], hb[1], hb[2], hb[3]);
		Render.rounded(g, hb[0], hb[1], hb[2], hb[3], hoverHud ? 0xFF26262F : 0xFF16161E);
		g.centeredText(font, Component.literal("HUD-Editor"), width / 2, hb[1] + 4, 0xFFD0D0DC);
	}

	public void open(int i) {
		switch (i) {
			case 0 -> minecraft.gui.setScreen(new ClickGuiScreen(this));
			case 1 -> minecraft.gui.setScreen(new CosmeticsScreen(this));
			case 2 -> minecraft.gui.setScreen(new FriendsScreen(this));
			case 3 -> minecraft.gui.setScreen(new MusicScreen(this));
			default -> minecraft.gui.setScreen(new HudEditorScreen(this));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == LEFT) {
			for (int i = 0; i < CARDS; i++) {
				int[] c = card(i);
				if (Render.inside(event.x(), event.y(), c[0], c[1], c[2], c[3])) {
					open(i);
					return true;
				}
			}
			int[] hb = hudButton();
			if (Render.inside(event.x(), event.y(), hb[0], hb[1], hb[2], hb[3])) {
				open(4);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == InputConstants.KEY_1 || key == InputConstants.KEY_NUMPAD1) {
			open(0);
			return true;
		}
		if (key == InputConstants.KEY_2 || key == InputConstants.KEY_NUMPAD2) {
			open(1);
			return true;
		}
		if (key == InputConstants.KEY_3 || key == InputConstants.KEY_NUMPAD3) {
			open(2);
			return true;
		}
		if (key == InputConstants.KEY_4 || key == InputConstants.KEY_NUMPAD4) {
			open(3);
			return true;
		}
		return super.keyPressed(event);
	}

	/** Test helper. */
	public int[] cardCenter(int i) {
		int[] c = card(i);
		return new int[] {(c[0] + c[2]) / 2, (c[1] + c[3]) / 2};
	}
}
