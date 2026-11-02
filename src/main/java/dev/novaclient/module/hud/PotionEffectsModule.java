package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public class PotionEffectsModule extends HudModule {
	private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	public final BoolSetting hideVanilla = add(new BoolSetting("Vanilla-Anzeige ausblenden", true));

	public PotionEffectsModule() {
		super("Trankeffekte", "Aktive Effekte mit Restzeit", true, Anchor.END, 2, Anchor.START, 84);
	}

	private record Line(String name, String time, int color) {
	}

	private List<Line> lines(boolean editor) {
		List<Line> out = new ArrayList<>();
		LocalPlayer p = mc().player;
		if (p != null) {
			for (MobEffectInstance e : p.getActiveEffects()) {
				String name = e.getEffect().value().getDisplayName().getString();
				int amp = e.getAmplifier();
				if (amp > 0) {
					name += " " + (amp < ROMAN.length ? ROMAN[amp] : Integer.toString(amp + 1));
				}
				out.add(new Line(name, formatDuration(e), 0xFF000000 | (e.getEffect().value().getColor() & 0xFFFFFF)));
			}
		}
		if (out.isEmpty() && editor) {
			out.add(new Line("Speed II", "1:30", 0xFF33EBFF));
			out.add(new Line("Strength", "0:45", 0xFFFFC700));
		}
		return out;
	}

	private static String formatDuration(MobEffectInstance e) {
		if (e.isInfiniteDuration()) {
			return "**:**";
		}
		int seconds = Math.max(0, e.getDuration() / 20);
		return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
	}

	@Override
	public int getWidth(boolean editor) {
		List<Line> lines = lines(editor);
		if (lines.isEmpty()) {
			return 0;
		}
		int w = 0;
		for (Line l : lines) {
			w = Math.max(w, font().width(l.name() + " " + l.time()));
		}
		return w + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		List<Line> lines = lines(editor);
		if (lines.isEmpty()) {
			return 0;
		}
		return lines.size() * (font().lineHeight + 1) + PADDING * 2 - 1;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		int ty = y + PADDING;
		for (Line l : lines(editor)) {
			text(g, l.name() + " ", x + PADDING, ty, l.color());
			text(g, l.time(), x + PADDING + font().width(l.name() + " "), ty, 0xFFAAAAAA);
			ty += font().lineHeight + 1;
		}
	}
}
