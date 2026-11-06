package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.LyricsModule;
import dev.novaclient.module.hud.MusicModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.music.CoverArt;
import dev.novaclient.music.Lyrics;
import dev.novaclient.music.MusicData;
import dev.novaclient.music.NowPlaying;
import dev.novaclient.network.NovaNetwork;
import dev.novaclient.util.Render;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The music menu (start menu, key 4): the current song with cover, time and player controls, the full synced lyrics
 * (click the line that is being sung to fix the timing), lyrics offset and choice per song, the songs you heard,
 * your most played songs and what other Nova players near you are listening to.
 */
public class MusicScreen extends Screen {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final String[] TABS = {"Songtext", "Verlauf", "Top-Songs", "Mitspieler"};
	private static final int LEFT_W = 156;
	private static final int LINE_H = 13;

	private record Button(String label, int x0, int y0, int x1, int y1, Runnable action) {
	}

	private final @Nullable Screen parent;
	private final List<Button> buttons = new ArrayList<>();
	private final List<int[]> lineBoxes = new ArrayList<>();
	private int tab;
	private float scroll;
	private float lyricsScroll = -1;
	private long userScrolledAt;
	private String message = "";
	private long messageAt;
	private int left;
	private int top;
	private int winW;
	private int winH;

	public MusicScreen(@Nullable Screen parent) {
		super(Component.literal("Musik"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		MusicData.saveIfNeeded(true);
		minecraft.gui.setScreen(parent);
	}

	private void layout() {
		winW = Math.min(width - 16, 470);
		winH = Math.min(height - 16, 280);
		left = (width - winW) / 2;
		top = (height - winH) / 2;
	}

	private void say(String text) {
		message = text;
		messageAt = System.currentTimeMillis();
	}

	private static boolean detecting() {
		Module music = NovaClient.modules().byName("Musik");
		Module lyrics = NovaClient.modules().byName("Songtext");
		return music != null && music.isEnabled() || lyrics != null && lyrics.isEnabled();
	}

	private int rightX0() {
		return left + LEFT_W + 14;
	}

	private int rightX1() {
		return left + winW - 8;
	}

	private int listTop() {
		return top + 40;
	}

	private int listBottom() {
		return top + winH - 22;
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		layout();
		buttons.clear();
		lineBoxes.clear();
		int accent = ThemeModule.accent();
		Render.rounded(g, left, top, left + winW, top + winH, Render.alpha(0xFF0E0E14, ThemeModule.menuAlpha()));
		Render.roundedOutline(g, left, top, left + winW, top + winH, Render.alpha(accent, 0x70));
		g.text(font, "♪ MUSIK", left + 10, top + 8, 0xFFFFFFFF, false);

		// tabs
		int tx = rightX0();
		for (int i = 0; i < TABS.length; i++) {
			int w = font.width(TABS[i]) + 8;
			int ti = i;
			boolean active = tab == i;
			boolean hover = Render.inside(mouseX, mouseY, tx, top + 6, tx + w, top + 20);
			Render.rounded(g, tx, top + 6, tx + w, top + 20, active ? accent : hover ? 0xFF26262F : 0xFF16161E);
			g.centeredText(font, Component.literal(TABS[i]), tx + w / 2, top + 9, active ? 0xFFFFFFFF : 0xFFB8B8C8);
			buttons.add(new Button("tab:" + TABS[i], tx, top + 6, tx + w, top + 20, () -> {
				tab = ti;
				scroll = 0;
			}));
			tx += w + 3;
		}

		NowPlaying.Song s = NowPlaying.current();
		drawSong(g, s, mouseX, mouseY, accent);
		g.fill(rightX0() - 7, top + 28, rightX0() - 6, top + winH - 10, 0x30FFFFFF);
		switch (tab) {
			case 0 -> drawLyrics(g, s, mouseX, mouseY, accent);
			case 1 -> drawRecent(g);
			case 2 -> drawTop(g, accent);
			default -> drawPlayers(g);
		}

		String foot = !message.isEmpty() && System.currentTimeMillis() - messageAt < 5000 ? message : footer(s);
		g.text(font, font.plainSubstrByWidth(foot, winW - 20), left + 10, top + winH - 13, 0xFF8E8EA4, false);
	}

	private String footer(NowPlaying.@Nullable Song s) {
		if (!detecting()) {
			return "Song-Erkennung ist aus: schalte das Modul „Musik“ oder „Songtext“ ein.";
		}
		if (tab == 0 && s != null && Lyrics.get(s) != null) {
			return "Tipp: Klick auf die Zeile, die gerade gesungen wird – dann passt der Songtext.";
		}
		return "Spotify, Browser und andere Player werden erkannt. Tasten für Play/Pause im Modul „Musik“.";
	}

	private void button(GuiGraphicsExtractor g, String label, int x0, int y0, int x1, int y1, int mouseX, int mouseY, int accent,
		boolean primary, Runnable action) {
		boolean hover = Render.inside(mouseX, mouseY, x0, y0, x1, y1);
		int bg = primary ? (hover ? Render.mix(accent, 0xFFFFFFFF, 0.15F) : accent) : hover ? 0xFF2A2A35 : 0xFF1A1A22;
		Render.rounded(g, x0, y0, x1, y1, bg);
		String shown = font.plainSubstrByWidth(label, x1 - x0 - 4);
		g.centeredText(font, Component.literal(shown), (x0 + x1) / 2, (y0 + y1) / 2 - 4, 0xFFFFFFFF);
		buttons.add(new Button(label, x0, y0, x1, y1, action));
	}

	private void drawSong(GuiGraphicsExtractor g, NowPlaying.@Nullable Song s, int mouseX, int mouseY, int accent) {
		int x = left + 10;
		int y = top + 26;
		int size = 56;
		if (!detecting()) {
			g.text(font, "Song-Erkennung ist aus.", x, y + 2, 0xFFB8B8C8, false);
			button(g, "Musik-Modul einschalten", x, y + 16, x + LEFT_W - 10, y + 32, mouseX, mouseY, accent, true, () -> {
				Module music = NovaClient.modules().byName("Musik");
				if (music != null) {
					music.setEnabled(true);
				}
				say("Musik-Modul ist an. Der Song erscheint in ein paar Sekunden.");
			});
			return;
		}
		if (s == null) {
			Render.rounded(g, x, y, x + size, y + size, 0xFF1A1A22);
			g.centeredText(font, Component.literal("♪"), x + size / 2, y + size / 2 - 4, 0xFF55556A);
			g.text(font, "Gerade läuft nichts.", x + size + 6, y + 4, 0xFFB8B8C8, false);
			g.text(font, font.plainSubstrByWidth(NowPlaying.status(), LEFT_W - size - 8), x + size + 6, y + 16, 0xFF6C6C7E, false);
		} else {
			Identifier cover = CoverArt.get(s.title(), s.artist());
			if (cover != null) {
				g.blit(RenderPipelines.GUI_TEXTURED, cover, x, y, 0, 0, size, size, 64, 64, 64, 64);
			} else {
				Render.rounded(g, x, y, x + size, y + size, Render.alpha(accent, 0xC0));
				g.pose().pushMatrix();
				g.pose().translate(x + size / 2F, y + size / 2F);
				g.pose().scale(2.5F, 2.5F);
				g.centeredText(font, Component.literal("♪"), 0, -4, 0xFFFFFFFF);
				g.pose().popMatrix();
			}
			int tx = x + size + 6;
			int tw = LEFT_W - size - 8;
			List<String> titleLines = wrap(s.title(), tw, 3);
			int ty = y + 1;
			for (String l : titleLines) {
				g.text(font, l, tx, ty, 0xFFFFFFFF, false);
				ty += 10;
			}
			g.text(font, font.plainSubstrByWidth(s.artist(), tw), tx, ty + 1, 0xFFB8B8C8, false);
			g.text(font, font.plainSubstrByWidth(s.source() + (s.playing() ? "" : " · pausiert"), tw), tx, ty + 12, 0xFF6C6C7E, false);
			if (s.hasTimeline()) {
				int by = y + size + 6;
				float f = s.lengthMs() > 0 ? s.positionNow() / (float) s.lengthMs() : 0;
				g.fill(x, by, x + LEFT_W - 10, by + 2, 0x40FFFFFF);
				g.fill(x, by, x + Math.round((LEFT_W - 10) * f), by + 2, accent);
				g.text(font, NowPlaying.Song.time(s.positionNow()), x, by + 5, 0xFF8E8EA4, false);
				String len = NowPlaying.Song.time(s.lengthMs());
				g.text(font, len, x + LEFT_W - 10 - font.width(len), by + 5, 0xFF8E8EA4, false);
			}
		}
		// player controls
		int cy = y + size + 22;
		int bw = (LEFT_W - 10 - 8) / 3;
		button(g, "⏮", x, cy, x + bw, cy + 16, mouseX, mouseY, accent, false, () -> control(NowPlaying.Control.PREVIOUS));
		button(g, s != null && s.playing() ? "⏸" : "▶", x + bw + 4, cy, x + 2 * bw + 4, cy + 16, mouseX, mouseY, accent, true,
			() -> control(NowPlaying.Control.PLAY_PAUSE));
		button(g, "⏭", x + 2 * bw + 8, cy, x + 3 * bw + 8, cy + 16, mouseX, mouseY, accent, false, () -> control(NowPlaying.Control.NEXT));
		if (s == null) {
			return;
		}

		// lyrics of this song
		int ly = cy + 24;
		LyricsModule lm = LyricsModule.get();
		String key = Lyrics.key(s);
		long songOffset = MusicData.offset(key);
		g.text(font, "Songtext: " + LyricsModule.describeOffset(songOffset), x, ly, 0xFFD0D0DC, false);
		int w3 = (LEFT_W - 10 - 8) / 3;
		button(g, "früher", x, ly + 11, x + w3, ly + 25, mouseX, mouseY, accent, false, () -> LyricsModule.nudge(s, LyricsModule.NUDGE_MS));
		button(g, "später", x + w3 + 4, ly + 11, x + 2 * w3 + 4, ly + 25, mouseX, mouseY, accent, false,
			() -> LyricsModule.nudge(s, -LyricsModule.NUDGE_MS));
		button(g, "0", x + 2 * w3 + 8, ly + 11, x + 3 * w3 + 8, ly + 25, mouseX, mouseY, accent, false, () -> {
			MusicData.setOffset(key, 0);
			say("Versatz für diesen Song zurückgesetzt.");
		});
		int choices = Lyrics.choices(s);
		boolean hidden = MusicData.hidden(key);
		int hw = (LEFT_W - 10 - 4) / 2;
		button(g, choices > 1 ? "Anderer Songtext (" + choices + ")" : "Anderer Songtext", x, ly + 29, x + LEFT_W - 10, ly + 43,
			mouseX, mouseY, accent, false, () -> say(Lyrics.next(s)));
		button(g, hidden ? "Text zeigen" : "Text aus", x, ly + 47, x + hw, ly + 61, mouseX, mouseY, accent, false, () -> {
			MusicData.setHidden(key, !hidden);
			say(hidden ? "Songtext für diesen Song wieder an." : "Songtext für diesen Song ausgeblendet.");
		});
		button(g, "Neu laden", x + hw + 4, ly + 47, x + 2 * hw + 4, ly + 61, mouseX, mouseY, accent, false, () -> {
			Lyrics.reload(s);
			say("Songtext wird neu gesucht …");
		});
		if (lm != null && !lm.isEnabled()) {
			button(g, "Songtext am Himmel an", x, ly + 65, x + LEFT_W - 10, ly + 79, mouseX, mouseY, accent, true, () -> {
				lm.setEnabled(true);
				say("Songtext ist an.");
			});
		}
	}

	private void control(NowPlaying.Control c) {
		if (!detecting()) {
			say("Schalte zuerst das Modul „Musik“ ein.");
			return;
		}
		NowPlaying.control(c);
		say(switch (c) {
			case PLAY_PAUSE -> "Play/Pause gesendet.";
			case NEXT -> "Nächster Song …";
			case PREVIOUS -> "Vorheriger Song …";
		});
	}

	private List<String> wrap(String text, int width, int maxLines) {
		List<String> out = new ArrayList<>();
		String rest = text;
		while (!rest.isEmpty() && out.size() < maxLines) {
			if (font.width(rest) <= width) {
				out.add(rest);
				break;
			}
			String part = font.plainSubstrByWidth(rest, width);
			int space = part.lastIndexOf(' ');
			if (space > 0 && out.size() < maxLines - 1) {
				part = part.substring(0, space);
			}
			if (out.size() == maxLines - 1 && font.width(rest) > width) {
				part = font.plainSubstrByWidth(rest, width - font.width("…")) + "…";
			}
			out.add(part);
			rest = rest.substring(Math.min(rest.length(), part.endsWith("…") ? rest.length() : part.length())).stripLeading();
		}
		return out;
	}

	private void drawLyrics(GuiGraphicsExtractor g, NowPlaying.@Nullable Song s, int mouseX, int mouseY, int accent) {
		int x0 = rightX0();
		int x1 = rightX1();
		int y0 = listTop() - 12;
		if (s == null) {
			g.text(font, "Starte einen Song in Spotify, im Browser …", x0, y0 + 4, 0xFF8E8EA4, false);
			return;
		}
		Lyrics.Track t = MusicData.hidden(Lyrics.key(s)) ? null : Lyrics.get(s);
		String info = Lyrics.info(s);
		if (!info.isEmpty()) {
			g.text(font, font.plainSubstrByWidth("lrclib " + info, x1 - x0), x0, y0, 0xFF55556A, false);
		}
		if (t == null || t.lines().isEmpty()) {
			String text = MusicData.hidden(Lyrics.key(s)) ? "Songtext für diesen Song ausgeblendet."
				: t != null && t.instrumental() ? "♪ Instrumental ♪" : switch (Lyrics.state(s)) {
					case LOADING -> "Songtext wird geladen …";
					case PLAIN_ONLY -> "Nur ein Songtext ohne Zeiten gefunden.";
					default -> "Kein passender Songtext gefunden.";
				};
			g.text(font, text, x0, listTop() + 6, 0xFFB8B8C8, false);
			return;
		}
		LyricsModule lm = LyricsModule.get();
		long pos = s.hasTimeline() ? s.positionNow() + (lm != null ? lm.totalOffset(s) : 0) : -1;
		int current = pos >= 0 ? t.indexAt(pos + LyricsModule.LEAD_MS) : -1;
		int areaH = listBottom() - listTop();
		// keep the current line in the upper third (smoothly), unless you scrolled yourself a moment ago
		float target = current >= 0 ? current * LINE_H - areaH / 3F : 0;
		float max = Math.max(0, t.lines().size() * LINE_H - areaH + 6);
		if (System.currentTimeMillis() - userScrolledAt > 4000) {
			lyricsScroll = lyricsScroll < 0 ? target : lyricsScroll + (target - lyricsScroll) * 0.15F;
		}
		lyricsScroll = Mth.clamp(lyricsScroll, 0, max);
		g.enableScissor(x0 - 2, listTop(), x1 + 2, listBottom());
		int y = listTop() + 2 - Math.round(lyricsScroll);
		for (int i = 0; i < t.lines().size(); i++) {
			Lyrics.Line l = t.lines().get(i);
			if (y + LINE_H >= listTop() && y <= listBottom()) {
				String text = l.isBreak() ? "♪" : l.text();
				boolean hover = Render.inside(mouseX, mouseY, x0, y - 1, x1, y + LINE_H - 2) && mouseY >= listTop() && mouseY <= listBottom();
				if (hover && pos >= 0) {
					Render.rounded(g, x0 - 2, y - 2, x1 + 2, y + LINE_H - 2, 0xFF1E1E28);
				}
				int color = i == current ? accent : i < current ? 0xFF8E8EA4 : 0xFFD8D8E4;
				if (i == current) {
					String shown = font.plainSubstrByWidth(text, Math.round((x1 - x0) / 1.1F));
					g.pose().pushMatrix();
					g.pose().translate(x0, y);
					g.pose().scale(1.1F, 1.1F);
					g.text(font, shown, 0, 0, color, true);
					g.pose().popMatrix();
				} else {
					g.text(font, font.plainSubstrByWidth(text, x1 - x0), x0, y, color, false);
				}
				lineBoxes.add(new int[] {i, x0, y - 1, x1, y + LINE_H - 2});
			}
			y += LINE_H;
		}
		g.disableScissor();
	}

	/** Click on a line: it is being sung right now, so the lyrics of this song are moved to fit. */
	private void syncTo(NowPlaying.Song s, int line) {
		Lyrics.Track t = Lyrics.get(s);
		LyricsModule lm = LyricsModule.get();
		if (t == null || lm == null || !s.hasTimeline() || t.line(line) == null) {
			return;
		}
		String key = Lyrics.key(s);
		long global = lm.totalOffset(s) - MusicData.offset(key);
		long wanted = t.lines().get(line).startMs() - s.positionNow() - global;
		MusicData.setOffset(key, wanted);
		userScrolledAt = 0;
		say("Songtext angepasst: " + LyricsModule.describeOffset(MusicData.offset(key)) + " (nur dieser Song)");
	}

	private static String ago(long at) {
		long min = Math.max(0, (System.currentTimeMillis() - at) / 60_000);
		if (min < 1) {
			return "gerade eben";
		}
		if (min < 60) {
			return "vor " + min + " Min.";
		}
		long h = min / 60;
		if (h < 24) {
			return "vor " + h + " Std.";
		}
		return "vor " + (h / 24) + " Tag" + (h / 24 == 1 ? "" : "en");
	}

	private static String duration(long ms) {
		long min = ms / 60_000;
		return min < 60 ? min + " Min." : (min / 60) + " Std. " + (min % 60) + " Min.";
	}

	private void drawList(GuiGraphicsExtractor g, List<String[]> rows, String empty) {
		int x0 = rightX0();
		int x1 = rightX1();
		if (rows.isEmpty()) {
			g.text(font, empty, x0, listTop() + 4, 0xFF8E8EA4, false);
			return;
		}
		int rowH = 22;
		float max = Math.max(0, rows.size() * rowH - (listBottom() - listTop()));
		scroll = Mth.clamp(scroll, 0, max);
		g.enableScissor(x0 - 2, listTop() - 2, x1 + 2, listBottom());
		int y = listTop() - Math.round(scroll);
		for (String[] r : rows) {
			if (y + rowH >= listTop() && y <= listBottom()) {
				Render.rounded(g, x0 - 2, y - 2, x1 + 2, y + rowH - 4, 0xFF15151C);
				g.text(font, font.plainSubstrByWidth(r[0], x1 - x0 - 4), x0 + 2, y, 0xFFFFFFFF, false);
				g.text(font, font.plainSubstrByWidth(r[1], x1 - x0 - 4), x0 + 2, y + 9, 0xFF8E8EA4, false);
			}
			y += rowH;
		}
		g.disableScissor();
	}

	private void drawRecent(GuiGraphicsExtractor g) {
		List<String[]> rows = new ArrayList<>();
		for (MusicData.Played p : MusicData.recent()) {
			rows.add(new String[] {p.title(), (p.artist().isEmpty() ? "" : p.artist() + " · ") + (p.source().isEmpty() ? "" : p.source() + " · ") + ago(p.at())});
		}
		drawList(g, rows, "Noch keine Songs gehört (Verlauf im Modul „Musik“).");
	}

	private void drawTop(GuiGraphicsExtractor g, int accent) {
		List<String[]> rows = new ArrayList<>();
		int i = 1;
		for (MusicData.Stat st : MusicData.top(30)) {
			rows.add(new String[] {"#" + i++ + "  " + st.title(),
				(st.artist().isEmpty() ? "" : st.artist() + " · ") + st.plays() + "× gehört · " + duration(st.listenedMs())});
		}
		g.text(font, "Insgesamt gehört: " + duration(MusicData.totalListened()), rightX0(), listTop() - 12, accent, false);
		drawList(g, rows, "Ein Song zählt, wenn du ihn 30 Sekunden lang hörst.");
	}

	private void drawPlayers(GuiGraphicsExtractor g) {
		List<String[]> rows = new ArrayList<>();
		if (minecraft.level != null) {
			for (AbstractClientPlayer p : minecraft.level.players()) {
				if (p == minecraft.player) {
					continue;
				}
				NowPlaying.Song song = NovaNetwork.songInfo(p.getUUID());
				if (song != null) {
					rows.add(new String[] {p.getName().getString(), "♪ " + MusicModule.format(song)});
				}
			}
		}
		drawList(g, rows, NovaNetwork.isConnected() ? "Gerade hört kein Nova-Spieler in deiner Nähe Musik."
			: "Nicht mit dem Nova-Netzwerk verbunden.");
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == LEFT) {
			for (Button b : List.copyOf(buttons)) {
				if (Render.inside(event.x(), event.y(), b.x0(), b.y0(), b.x1(), b.y1())) {
					b.action().run();
					return true;
				}
			}
			NowPlaying.Song s = NowPlaying.current();
			if (tab == 0 && s != null && event.y() >= listTop() && event.y() <= listBottom()) {
				for (int[] box : lineBoxes) {
					if (Render.inside(event.x(), event.y(), box[1], box[2], box[3], box[4])) {
						syncTo(s, box[0]);
						return true;
					}
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (tab == 0) {
			lyricsScroll = Math.max(0, lyricsScroll - (float) scrollY * 20);
			userScrolledAt = System.currentTimeMillis();
		} else {
			scroll = Math.max(0, scroll - (float) scrollY * 20);
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_4) {
			tab = key - InputConstants.KEY_1;
			scroll = 0;
			return true;
		}
		if (key == InputConstants.KEY_SPACE) {
			control(NowPlaying.Control.PLAY_PAUSE);
			return true;
		}
		return super.keyPressed(event);
	}

	// ------------------------------------------------------------------ test helpers

	/** Centre of the button with this label (as drawn in the last frame), or null. */
	public int @Nullable [] buttonCenter(String label) {
		for (Button b : buttons) {
			if (b.label().equals(label) || b.label().startsWith(label)) {
				return new int[] {(b.x0() + b.x1()) / 2, (b.y0() + b.y1()) / 2};
			}
		}
		return null;
	}

	/** Centre of a lyrics line (as drawn in the last frame), or null if not visible. */
	public int @Nullable [] lineCenter(int index) {
		for (int[] box : lineBoxes) {
			if (box[0] == index) {
				return new int[] {(box[1] + box[3]) / 2, (box[2] + box[4]) / 2};
			}
		}
		return null;
	}

	public int tab() {
		return tab;
	}
}
