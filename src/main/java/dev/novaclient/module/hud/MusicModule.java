package dev.novaclient.module.hud;

import dev.novaclient.music.NowPlaying;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.util.Render;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shows the song you are listening to: on the HUD, above your name and – via the Nova network –
 * above your name for all other Nova players.
 */
public class MusicModule extends HudModule {
	private static MusicModule instance;
	private static final int MAX_W = 170;

	public final BoolSetting aboveName = add(new BoolSetting("Über meinem Namen anzeigen", true));
	public final BoolSetting share = add(new BoolSetting("Mit anderen Nova-Spielern teilen", true));
	public final BoolSetting showHud = add(new BoolSetting("HUD anzeigen", true));
	public final BoolSetting onlyPlaying = add(new BoolSetting("Nur wenn Musik läuft", true));
	public final BoolSetting timestamp = add(new BoolSetting("Zeit anzeigen (1:23 / 3:45)", true));
	public final BoolSetting cover = add(new BoolSetting("Song-Cover anzeigen (iTunes-Suche)", true));

	public MusicModule() {
		super("Musik", "Aktueller Song (Spotify, Browser, …) im HUD und über deinem Namen", false, Anchor.START, 2, Anchor.END, 52);
		instance = this;
	}

	public static MusicModule get() {
		return instance;
	}

	@Override
	public void tick() {
		// the lyrics module uses the same song detection
		if (isEnabled() || LyricsModule.needsSong()) {
			NowPlaying.start();
		} else {
			NowPlaying.stop();
		}
	}

	@Override
	protected void onDisable() {
		if (!LyricsModule.needsSong()) {
			NowPlaying.stop();
		}
	}

	private NowPlaying.Song song() {
		NowPlaying.Song s = NowPlaying.current();
		if (s == null || onlyPlaying.isOn() && !s.playing()) {
			return null;
		}
		return s;
	}

	/** The song shown above the local player's name, or null. */
	public static NowPlaying.Song songAboveName() {
		MusicModule m = instance;
		if (m == null || !m.isEnabled() || !m.aboveName.isOn()) {
			return null;
		}
		return m.song();
	}

	/** Timestamps above names (yours and others') are shown; on by default, also when the module is off. */
	public static boolean showTimestamps() {
		return instance == null || instance.timestamp.isOn();
	}

	/** Covers above names (yours and others') are shown and looked up. */
	public static boolean showCovers() {
		return instance == null || instance.cover.isOn();
	}

	/** Song to send to other Nova players, or null. */
	public static NowPlaying.Song sharedSong() {
		MusicModule m = instance;
		if (m == null || !m.isEnabled() || !m.share.isOn()) {
			return null;
		}
		NowPlaying.Song s = NowPlaying.current();
		return s != null && s.playing() ? s : null;
	}

	public static String format(NowPlaying.Song s) {
		return s.artist().isEmpty() ? s.title() : s.title() + " – " + s.artist();
	}

	private NowPlaying.Song hudSong(boolean editor) {
		NowPlaying.Song s = song();
		if (s == null && editor) {
			return new NowPlaying.Song("Never Gonna Give You Up", "Rick Astley", true, "", 61_000, 213_000, System.currentTimeMillis());
		}
		return s;
	}

	private String[] lines(boolean editor) {
		NowPlaying.Song s = hudSong(editor);
		if (s == null) {
			return null;
		}
		return new String[] {s.title(), s.artist().isEmpty() ? s.source() : s.artist()};
	}

	private boolean bar(boolean editor) {
		NowPlaying.Song s = hudSong(editor);
		return s != null && s.hasTimeline() && timestamp.isOn();
	}

	@Override
	public int getWidth(boolean editor) {
		String[] l = lines(editor);
		if (!showHud.isOn() && !editor || l == null) {
			return 0;
		}
		int w = Math.max(font().width(l[0]), font().width(l[1]));
		return Math.max(110, Math.min(MAX_W, w)) + 24 + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return getWidth(editor) == 0 ? 0 : 2 * (font().lineHeight + 1) + PADDING * 2 + (bar(editor) ? 9 : 2);
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		String[] l = lines(editor);
		if (l == null) {
			return;
		}
		NowPlaying.Song s = hudSong(editor);
		int accent = ThemeModule.accent();
		int size = 20;
		int cx = x + PADDING;
		int cy = y + PADDING;
		net.minecraft.resources.Identifier coverTex = cover.isOn() ? dev.novaclient.music.CoverArt.get(s.title(), s.artist()) : null;
		if (coverTex != null) {
			g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, coverTex, cx, cy, 0, 0, size, size, 64, 64, 64, 64);
		} else {
			// note icon while the cover loads / if there is none
			Render.rounded(g, cx, cy, cx + size, cy + size, Render.alpha(accent, 0xD0));
			g.text(font(), "♪", cx + 7, cy + 6, 0xFFFFFFFF, false);
		}
		int tx = x + PADDING + size + 4;
		text(g, ellipsis(l[0]), tx, y + PADDING + 1, 0xFFFFFFFF);
		text(g, ellipsis(l[1]), tx, y + PADDING + 11, 0xFFA8A8B8);
		if (bar(editor)) {
			int by = y + PADDING + 23;
			int bx0 = x + PADDING;
			int bx1 = x + getWidth(editor) - PADDING;
			String t0 = NowPlaying.Song.time(s.positionNow());
			String t1 = NowPlaying.Song.time(s.lengthMs());
			int barX0 = bx0 + font().width(t0) + 4;
			int barX1 = bx1 - font().width(t1) - 4;
			g.pose().pushMatrix();
			g.pose().translate(bx0, by);
			g.pose().scale(0.75F, 0.75F);
			g.text(font(), t0, 0, 0, 0xFFA8A8B8, false);
			g.pose().popMatrix();
			g.pose().pushMatrix();
			g.pose().translate(bx1 - font().width(t1) * 0.75F, by);
			g.pose().scale(0.75F, 0.75F);
			g.text(font(), t1, 0, 0, 0xFFA8A8B8, false);
			g.pose().popMatrix();
			barX0 = bx0 + (int) (font().width(t0) * 0.75F) + 4;
			barX1 = bx1 - (int) (font().width(t1) * 0.75F) - 4;
			float f = s.lengthMs() > 0 ? s.positionNow() / (float) s.lengthMs() : 0;
			g.fill(barX0, by + 2, barX1, by + 4, 0x40FFFFFF);
			g.fill(barX0, by + 2, barX0 + Math.round((barX1 - barX0) * f), by + 4, accent);
		}
	}

	private static String ellipsis(String s) {
		if (font().width(s) <= MAX_W) {
			return s;
		}
		return font().plainSubstrByWidth(s, MAX_W - font().width("…")) + "…";
	}
}
