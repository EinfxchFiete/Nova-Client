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

	public MusicModule() {
		super("Musik", "Aktueller Song (Spotify, Browser, …) im HUD und über deinem Namen", false, Anchor.START, 2, Anchor.END, 52);
		instance = this;
	}

	public static MusicModule get() {
		return instance;
	}

	@Override
	public void tick() {
		if (isEnabled()) {
			NowPlaying.start();
		} else {
			NowPlaying.stop();
		}
	}

	@Override
	protected void onDisable() {
		NowPlaying.stop();
	}

	private NowPlaying.Song song() {
		NowPlaying.Song s = NowPlaying.current();
		if (s == null || onlyPlaying.isOn() && !s.playing()) {
			return null;
		}
		return s;
	}

	/** Text for the line above the local player's name, or null. */
	public static String songAboveName() {
		MusicModule m = instance;
		if (m == null || !m.isEnabled() || !m.aboveName.isOn()) {
			return null;
		}
		NowPlaying.Song s = m.song();
		return s == null ? null : format(s);
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

	private String[] lines(boolean editor) {
		NowPlaying.Song s = song();
		if (s == null) {
			return editor ? new String[] {"Never Gonna Give You Up", "Rick Astley"} : null;
		}
		return new String[] {s.title(), s.artist().isEmpty() ? s.source() : s.artist()};
	}

	@Override
	public int getWidth(boolean editor) {
		String[] l = lines(editor);
		if (!showHud.isOn() && !editor || l == null) {
			return 0;
		}
		int w = Math.max(font().width(l[0]), font().width(l[1]));
		return Math.min(MAX_W, w) + 18 + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return getWidth(editor) == 0 ? 0 : 2 * (font().lineHeight + 1) + PADDING * 2;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		String[] l = lines(editor);
		if (l == null) {
			return;
		}
		int accent = ThemeModule.accent();
		// note icon
		Render.rounded(g, x + PADDING, y + PADDING + 1, x + PADDING + 14, y + PADDING + 17, Render.alpha(accent, 0xD0));
		g.text(font(), "♪", x + PADDING + 4, y + PADDING + 5, 0xFFFFFFFF, false);
		int tx = x + PADDING + 18;
		text(g, ellipsis(l[0]), tx, y + PADDING + 1, 0xFFFFFFFF);
		text(g, ellipsis(l[1]), tx, y + PADDING + 11, 0xFFA8A8B8);
	}

	private static String ellipsis(String s) {
		if (font().width(s) <= MAX_W) {
			return s;
		}
		return font().plainSubstrByWidth(s, MAX_W - font().width("…")) + "…";
	}
}
