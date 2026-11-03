package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;

/** A small X around the crosshair (and an optional click sound) when you hit something. */
public class HitMarkerModule extends Module {
	private static HitMarkerModule instance;
	private static final long SHOW_MS = 250;

	public final BoolSetting sound = add(new BoolSetting("Treffer-Ton", false));
	private long lastHit;

	public HitMarkerModule() {
		super("Trefferanzeige", "Kleines X am Fadenkreuz, wenn du etwas triffst", Category.VISUAL, false);
		instance = this;
	}

	/** Called when you attack an entity. */
	public static void onHit() {
		HitMarkerModule m = instance;
		if (m == null || !m.isEnabled()) {
			return;
		}
		m.lastHit = System.currentTimeMillis();
		Minecraft mc = Minecraft.getInstance();
		if (m.sound.isOn() && mc.player != null) {
			mc.player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.6F, 1.8F);
		}
	}

	public static boolean visible() {
		HitMarkerModule m = instance;
		return m != null && m.isEnabled() && System.currentTimeMillis() - m.lastHit < SHOW_MS;
	}

	public static void render(GuiGraphicsExtractor g, int w, int h) {
		if (!visible()) {
			return;
		}
		int cx = w / 2;
		int cy = h / 2;
		int c = ThemeModule.accent() | 0xFF000000;
		for (int i = 3; i <= 6; i++) {
			g.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, c);
			g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, c);
			g.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, c);
			g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, c);
		}
	}
}
