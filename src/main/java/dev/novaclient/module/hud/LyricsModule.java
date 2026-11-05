package dev.novaclient.module.hud;

import dev.novaclient.music.CoverArt;
import dev.novaclient.music.Lyrics;
import dev.novaclient.music.NowPlaying;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.util.Render;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * "Songtext": the synced lyrics of the current song as karaoke text, big in the sky in front of you
 * ({@link dev.novaclient.music.LyricsSky}) and/or on the HUD. Lyrics come from LRCLIB; only title, artist and
 * length of the song are sent. Uses the same song detection as the music module.
 */
public class LyricsModule extends HudModule {
	private static LyricsModule instance;
	private static final int HUD_W = 300;
	private static final int HUD_H = 38;
	private static final int UNSUNG = 0x8CFFFFFF;

	public final BoolSetting sky = add(new BoolSetting("Am Himmel anzeigen", true));
	public final BoolSetting hud = add(new BoolSetting("Im HUD anzeigen", false));
	public final ModeSetting color = add(new ModeSetting("Farbe", "Theme", "Theme", "Cover", "Regenbogen", "Weiß"));
	public final NumberSetting size = add(new NumberSetting("Größe", 1.0, 0.5, 2.0, 0.1));
	public final NumberSetting height = add(new NumberSetting("Höhe am Himmel", 22, 5, 60, 1));
	public final BoolSetting follow = add(new BoolSetting("Folgt der Blickrichtung", true));
	public final BoolSetting throughBlocks = add(new BoolSetting("Durch Blöcke sichtbar", true));
	public final BoolSetting nextLine = add(new BoolSetting("Nächste Zeile zeigen", true));
	public final NumberSetting offset = add(new NumberSetting("Versatz (Sekunden)", 0.0, -3.0, 3.0, 0.1));

	/** What to show in this frame; shared by the sky and the HUD. */
	public record Frame(float alpha, NowPlaying.Song song, Lyrics.State state, Lyrics.@Nullable Track track, long pos,
		int current, int leaving, float change, float title, float dots, float dotsOut, int next, boolean paused) {
	}

	/** Draws text centred at a point of the lyrics layout (font pixels, y down, (0, 0) = the current line). */
	public interface Painter {
		void draw(String s, float cx, float cy, float scale, int argb, boolean shadow);

		int width(String s);
	}

	private long lastFrameAt;
	private float presence;
	private NowPlaying.@Nullable Song shownSong;
	private String songKey = "";
	private long songSince;
	private int shown = -1;
	private int leaving = -1;
	private long changedAt;
	private float titleAlpha;
	private long lastDotsAt;
	private @Nullable Frame cached;
	private long cachedAt = -1;

	public LyricsModule() {
		super("Songtext", "Songtext zum aktuellen Song wie Karaoke – groß am Himmel vor dir (Texte von lrclib.net)", false,
			Anchor.CENTER, 0, Anchor.START, 40);
		instance = this;
		// lyrics on the HUD look best without a box
		background.set(false);
	}

	@Override
	public void resetToDefaults() {
		super.resetToDefaults();
		background.set(false);
	}

	public static LyricsModule get() {
		return instance;
	}

	/** True if the module needs the song detection running. */
	public static boolean needsSong() {
		return instance != null && instance.isEnabled();
	}

	/** Title + artist of the song shown at the moment ("" if none). */
	public String songKey() {
		return songKey;
	}

	// ------------------------------------------------------------------ timing

	private static float approach(float value, float target, float dt, float speed) {
		return value + (target - value) * (1 - (float) Math.exp(-dt * speed));
	}

	/** End of the karaoke fill of a line: lines are sung faster than the gap to the next line. */
	public static long fillEnd(Lyrics.Line l, long nextStart) {
		long gap = nextStart > l.startMs() ? nextStart - l.startMs() : 6000;
		int letters = 0;
		for (int i = 0; i < l.text().length(); i++) {
			if (!Character.isWhitespace(l.text().charAt(i))) {
				letters++;
			}
		}
		long dur = Math.min(Math.round(gap * 0.9), Math.max(650L, letters * 72L));
		long[] words = l.wordMs();
		if (words != null && words.length > 0) {
			dur = Math.max(dur, words[words.length - 1] - l.startMs() + 300);
		}
		return l.startMs() + dur;
	}

	/** Sung characters of the line (fractional, in chars of {@code l.text()}) at {@code pos}. */
	public static float fill(Lyrics.Line l, long pos, long nextStart) {
		int len = l.text().length();
		if (pos <= l.startMs()) {
			return 0;
		}
		int[] wc = l.wordChars();
		long[] wm = l.wordMs();
		long end = fillEnd(l, nextStart);
		if (wc != null && wm != null && wc.length == wm.length && wc.length > 0) {
			// word timings: fill each word between its start and the start of the next word
			int k = -1;
			for (int i = 0; i < wm.length; i++) {
				if (wm[i] <= pos) {
					k = i;
				}
			}
			if (k < 0) {
				return 0;
			}
			int c0 = Math.min(len, wc[k]);
			int c1 = k + 1 < wc.length ? Math.min(len, wc[k + 1]) : len;
			long t0 = wm[k];
			long t1 = k + 1 < wm.length ? wm[k + 1] : Math.max(t0 + 200, end);
			t1 = Math.min(t1, t0 + Math.max(250, (c1 - c0) * 130L));
			float f = t1 > t0 ? Mth.clamp((pos - t0) / (float) (t1 - t0), 0, 1) : 1;
			return c0 + (c1 - c0) * f;
		}
		return len * Mth.clamp((pos - l.startMs()) / (float) (end - l.startMs()), 0, 1);
	}

	private static int nextText(Lyrics.Track t, int from) {
		for (int i = Math.max(0, from); i < t.lines().size(); i++) {
			if (!t.lines().get(i).isBreak()) {
				return i;
			}
		}
		return -1;
	}

	/** The state of this frame, or null if there is nothing to show. */
	public @Nullable Frame frame() {
		long now = System.currentTimeMillis();
		if (now == cachedAt) {
			return cached;
		}
		cachedAt = now;
		float dt = lastFrameAt == 0 ? 0 : Math.min(0.25F, (now - lastFrameAt) / 1000F);
		lastFrameAt = now;
		NowPlaying.Song song = NowPlaying.current();
		if (song != null && song.title().isBlank()) {
			song = null;
		}
		if (song != null) {
			String key = song.title() + "\u0000" + song.artist();
			if (!key.equals(songKey)) {
				songKey = key;
				songSince = now;
				shown = -1;
				leaving = -1;
				changedAt = now;
				titleAlpha = 0;
				lastDotsAt = 0;
			}
			shownSong = song;
		}
		presence = approach(presence, song != null ? 1 : 0, dt, song != null ? 5 : 3.5F);
		if (song == null && presence < 0.01F) {
			presence = 0;
			shownSong = null;
			songKey = "";
		}
		NowPlaying.Song s = shownSong;
		if (s == null) {
			cached = null;
			return null;
		}
		long pos = s.hasTimeline() ? s.positionNow() + Math.round(offset.get() * 1000) : -1;
		Lyrics.State state = Lyrics.state(s.title(), s.artist(), s.lengthMs());
		Lyrics.Track track = state == Lyrics.State.READY ? Lyrics.get(s.title(), s.artist(), s.lengthMs()) : null;
		boolean synced = track != null && !track.instrumental() && !track.lines().isEmpty() && pos >= 0;

		int display = -1;
		float dots = -1;
		int next = -1;
		if (synced) {
			int idx = track.indexAt(pos);
			Lyrics.Line line = track.line(idx);
			next = nextText(track, idx + 1);
			long nextAt = next >= 0 ? track.lines().get(next).startMs() : -1;
			if (line != null && !line.isBreak()) {
				long end = fillEnd(line, track.nextStart(idx));
				boolean longPause = nextAt < 0 ? pos > end + 4000 : nextAt - end > 6000;
				if (longPause && pos > end + 2500) {
					// sung and a long instrumental part follows: three dots count down to the next line
					if (nextAt > 0) {
						dots = Mth.clamp((pos - end - 2500) / (float) (nextAt - end - 2500), 0, 1);
					}
				} else {
					display = idx;
				}
			} else if (nextAt > 0) {
				long from = line == null ? Math.max(0, nextAt - 4000) : line.startMs();
				if (nextAt - from > 1200 && pos >= from) {
					dots = Mth.clamp((pos - from) / (float) (nextAt - from), 0, 1);
				}
			}
		}
		if (display != shown) {
			leaving = shown;
			shown = display;
			changedAt = now;
		}
		float change = Mth.clamp((now - changedAt) / 450F, 0, 1);
		if (dots >= 0) {
			lastDotsAt = now;
		}
		float dotsOut = dots < 0 && lastDotsAt > 0 && now - lastDotsAt < 450 ? (now - lastDotsAt) / 450F : -1;

		long since = now - songSince;
		float titleTarget;
		if (!synced) {
			boolean instrumental = track != null && track.instrumental();
			titleTarget = instrumental || state == Lyrics.State.LOADING || since < 9000 ? 1 : 0;
		} else {
			boolean beforeFirst = pos < track.lines().get(0).startMs() - 2500;
			titleTarget = beforeFirst || since < 4500 && display < 0 && dots < 0 ? 1 : 0;
		}
		titleAlpha = approach(titleAlpha, titleTarget, dt, 4);
		cached = new Frame(presence, s, state, track, pos, display, leaving, change, titleAlpha, dots, dotsOut, next, !s.playing());
		return cached;
	}

	/** Small text under the title while there are no lyrics to sing. */
	private static String status(Frame f) {
		if (f.track() != null && f.track().instrumental()) {
			return "♪ Instrumental ♪";
		}
		return switch (f.state()) {
			case LOADING -> "Songtext wird geladen …";
			case MISSING -> "Kein Songtext gefunden";
			case PLAIN_ONLY -> "Nur ein Songtext ohne Zeiten gefunden";
			default -> f.pos() < 0 ? "Dein Player sendet keine Zeit" : f.paused() ? "Pausiert" : "";
		};
	}

	// ------------------------------------------------------------------ colours

	private int baseColor(NowPlaying.Song s, int i, float time) {
		return switch (color.get()) {
			case "Regenbogen" -> 0xFF000000 | (Color.HSBtoRGB((time * 0.12F + i * 0.035F) % 1F, 0.68F, 1F) & 0xFFFFFF);
			case "Cover" -> {
				int c = CoverArt.color(s.title(), s.artist());
				yield c != 0 ? c : ThemeModule.accent();
			}
			case "Weiß" -> 0xFFFFFFFF;
			default -> ThemeModule.accent();
		};
	}

	/** Colour of a sung character: the chosen colour with a soft light running through it. */
	public int sungColor(NowPlaying.Song s, int i, float time) {
		float shimmer = 0.5F + 0.5F * Mth.sin(time * 3.2F - i * 0.33F);
		return Render.mix(baseColor(s, i, time), 0xFFFFFFFF, 0.06F + 0.22F * shimmer);
	}

	private static int fade(int argb, float alpha) {
		int a = Math.round((argb >>> 24) * Mth.clamp(alpha, 0, 1));
		return (argb & 0x00FFFFFF) | (a << 24);
	}

	private static float easeOut(float t) {
		float u = 1 - Mth.clamp(t, 0, 1);
		return 1 - u * u * u;
	}

	private static float easeOutBack(float t) {
		t = Mth.clamp(t, 0, 1);
		float c1 = 1.70158F;
		float c3 = c1 + 1;
		return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
	}

	private static float fit(Painter p, String s, float maxWidth) {
		int w = p.width(s);
		return w <= maxWidth || w == 0 ? 1 : maxWidth / w;
	}

	// ------------------------------------------------------------------ painting

	/** Paints the frame (sky and HUD use the same layout). */
	public void paint(Frame f, Painter p, float maxWidth) {
		NowPlaying.Song s = f.song();
		float a = f.alpha() * (f.paused() ? 0.55F : 1);
		float time = (System.currentTimeMillis() % 3_600_000L) / 1000F;
		Lyrics.Track t = f.track();
		// the line that was just sung floats up and fades out
		if (t != null && f.leaving() >= 0 && f.change() < 1) {
			Lyrics.Line l = t.line(f.leaving());
			if (l != null && !l.isBreak()) {
				float lf = easeOut(f.change());
				float sc = fit(p, l.text(), maxWidth) * (1 - 0.2F * lf);
				drawLine(p, l.text(), 0, -13 * lf, sc, sungColor(s, 0, time), a * (1 - lf));
			}
		}
		if (t != null && f.current() >= 0) {
			Lyrics.Line l = t.line(f.current());
			if (l != null) {
				karaoke(p, s, l, f.pos(), t.nextStart(f.current()), f.change(), a, time, maxWidth);
			}
		}
		if (f.dots() >= 0) {
			float in = f.current() < 0 ? easeOut(f.change()) : 1;
			for (int k = 0; k < 3; k++) {
				float lit = Mth.clamp(f.dots() * 3 - k, 0, 1);
				float sc = (0.8F + 0.45F * easeOut(lit)) * (1 + 0.07F * Mth.sin(time * 4.2F + k * 1.1F));
				int col = Render.mix(0x70FFFFFF, sungColor(s, k * 5, time), lit);
				p.draw("●", (k - 1) * 11, 0, sc * in, fade(col, a * in), true);
			}
		} else if (f.dotsOut() >= 0) {
			// the dots burst away when the next line starts
			float o = f.dotsOut();
			for (int k = 0; k < 3; k++) {
				p.draw("●", (k - 1) * (11 + 8 * o), 0, 1.25F + 0.7F * o, fade(sungColor(s, k * 5, time), a * (1 - o)), true);
			}
		}
		if (nextLine.isOn() && t != null && f.next() >= 0 && f.title() < 0.5F) {
			Lyrics.Line n = t.line(f.next());
			if (n != null) {
				float in = easeOut(f.change());
				float sc = 0.62F * fit(p, n.text(), maxWidth / 0.62F);
				float na = (f.current() >= 0 ? 0.42F : 0.58F) * a * in;
				p.draw(n.text(), 0, 12 + 5 * (1 - in), sc, fade(0xFFFFFFFF, na), true);
			}
		}
		if (f.title() > 0.01F) {
			float ta = a * f.title();
			float lift = -7 * (1 - f.title());
			String title = "♪ " + s.title();
			p.draw(title, 0, -14 + lift, 1.15F * fit(p, title, maxWidth / 1.15F), fade(sungColor(s, 0, time), ta), true);
			if (!s.artist().isEmpty()) {
				p.draw(s.artist(), 0, -3 + lift, 0.72F * fit(p, s.artist(), maxWidth / 0.72F), fade(0xFFDCDCE8, ta * 0.92F), true);
			}
			String status = status(f);
			if (!status.isEmpty()) {
				p.draw(status, 0, 6 + lift, 0.56F, fade(0xFFB4B4C4, ta * 0.8F), true);
			}
		}
	}

	private static void drawLine(Painter p, String text, float cx, float cy, float scale, int color, float alpha) {
		if (alpha > 0.02F) {
			p.draw(text, cx, cy, scale, fade(color, alpha), true);
		}
	}

	/** The current line: letters fill with colour as they are sung, the letters being sung hop up a little. */
	private void karaoke(Painter p, NowPlaying.Song s, Lyrics.Line l, long pos, long nextStart, float change, float a, float time, float maxWidth) {
		String text = l.text();
		float appear = change;
		float scale = fit(p, text, maxWidth) * (0.82F + 0.18F * easeOutBack(appear));
		float yBase = 9 * (1 - easeOut(appear));
		float alpha = a * Mth.clamp(appear * 1.7F, 0, 1);
		if (alpha < 0.02F) {
			return;
		}
		int len = text.length();
		float filled = fill(l, pos, nextStart);
		boolean singing = filled > 0 && filled < len;
		long end = fillEnd(l, nextStart);
		float flash = pos >= end ? Math.max(0, 1 - (pos - end) / 380F) : 0;
		float x = -p.width(text) / 2F;
		for (int off = 0; off < len; ) {
			int cp = text.codePointAt(off);
			int n = Character.charCount(cp);
			String c = text.substring(off, off + n);
			int w = p.width(c);
			if (!Character.isWhitespace(cp)) {
				float f = Mth.clamp(filled - off, 0, 1);
				float bump = singing ? Math.max(0, 1 - Math.abs(off + 0.5F - filled) / 2.4F) : 0;
				bump *= bump;
				float wave = Mth.sin(time * 2.4F + off * 0.42F) * 0.5F;
				int sung = sungColor(s, off, time);
				if (flash > 0) {
					sung = Render.mix(sung, 0xFFFFFFFF, 0.55F * flash);
				}
				int col = Render.mix(UNSUNG, sung, f);
				float cx = (x + w / 2F) * scale;
				float cy = yBase + (wave - 1.8F * bump - 0.8F * flash) * scale;
				p.draw(c, cx, cy, scale * (1 + 0.24F * bump + 0.06F * flash), fade(col, alpha), true);
			}
			x += w;
			off += n;
		}
	}

	// ------------------------------------------------------------------ HUD

	private boolean hudVisible(boolean editor) {
		return hud.isOn() && (editor || frame() != null);
	}

	@Override
	public int getWidth(boolean editor) {
		return hudVisible(editor) ? HUD_W : 0;
	}

	@Override
	public int getHeight(boolean editor) {
		return hudVisible(editor) ? HUD_H : 0;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		float ox = x + HUD_W / 2F;
		float oy = y + HUD_H / 2F;
		Painter p = new Painter() {
			@Override
			public void draw(String s, float cx, float cy, float scale, int argb, boolean dropShadow) {
				if ((argb >>> 24) < 6) {
					return;
				}
				g.pose().pushMatrix();
				g.pose().translate(ox + cx, oy + cy);
				g.pose().scale(scale, scale);
				g.pose().translate(-font().width(s) / 2F, -4);
				g.text(font(), s, 0, 0, argb, dropShadow && shadow.isOn());
				g.pose().popMatrix();
			}

			@Override
			public int width(String s) {
				return font().width(s);
			}
		};
		Frame f = frame();
		if (f == null) {
			if (editor) {
				p.draw("♪ Hier steht der Songtext", 0, 0, 1, ThemeModule.accent(), true);
			}
			return;
		}
		paint(f, p, HUD_W - 8);
	}
}
