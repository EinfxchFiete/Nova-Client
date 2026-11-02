package dev.novaclient.util;

import dev.novaclient.module.hud.ThemeModule;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small toasts shown above the hotbar (module toggles, warnings). */
public final class Notifications {
	private static final long DURATION = 2500L;
	private static final int MAX = 3;
	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static boolean enabled = true;

	private record Entry(String text, int color, long time) {
	}

	private Notifications() {
	}

	public static void setEnabled(boolean value) {
		enabled = value;
		if (!value) {
			ENTRIES.clear();
		}
	}

	public static void push(String text, int color) {
		if (!enabled) {
			return;
		}
		ENTRIES.removeIf(e -> e.text().equals(text));
		ENTRIES.add(new Entry(text, color, System.currentTimeMillis()));
		while (ENTRIES.size() > MAX) {
			ENTRIES.remove(0);
		}
	}

	public static List<String> active() {
		long now = System.currentTimeMillis();
		List<String> out = new ArrayList<>();
		for (Entry e : ENTRIES) {
			if (now - e.time() < DURATION) {
				out.add(e.text());
			}
		}
		return out;
	}

	public static void render(GuiGraphicsExtractor g) {
		long now = System.currentTimeMillis();
		Iterator<Entry> it = ENTRIES.iterator();
		while (it.hasNext()) {
			if (now - it.next().time() >= DURATION) {
				it.remove();
			}
		}
		if (ENTRIES.isEmpty()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		// top centre, below the compass bar; newest first
		int y = 42;
		for (int i = ENTRIES.size() - 1; i >= 0; i--) {
			Entry e = ENTRIES.get(i);
			long age = now - e.time();
			float alpha = age < 150 ? age / 150F : age > DURATION - 400 ? (DURATION - age) / 400F : 1F;
			int a = (int) (Math.max(0, Math.min(1, alpha)) * 255);
			if (a < 8) {
				continue;
			}
			int w = font.width(e.text()) + 16;
			int x = (g.guiWidth() - w) / 2;
			Render.rounded(g, x, y, x + w, y + 14, Render.alpha(0xFF101016, a * 220 / 255));
			g.fill(x + 3, y + 4, x + 5, y + 10, Render.alpha(e.color() == 0 ? ThemeModule.accent() : e.color(), a));
			g.text(font, e.text(), x + 10, y + 3, Render.alpha(0xFFFFFFFF, a), false);
			y += 16;
		}
	}
}
