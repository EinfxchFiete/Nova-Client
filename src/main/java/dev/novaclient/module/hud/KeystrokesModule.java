package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.util.CpsTracker;
import dev.novaclient.util.Render;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeystrokesModule extends HudModule {
	private static final int KEY = 22;
	private static final int MOUSE_H = 24;
	private static final int SPACE_H = 12;
	private static final int GAP = 2;
	private static final int WIDTH = KEY * 3 + GAP * 2;

	public final BoolSetting showMouse = add(new BoolSetting("Maustasten", true));
	public final BoolSetting showCps = add(new BoolSetting("CPS anzeigen", true));
	public final BoolSetting showSpace = add(new BoolSetting("Leertaste", true));
	public final ModeSetting pressedColor = add(new ModeSetting("Farbe beim Drücken", "Weiß", "Weiß", "Theme"));

	/** 0..1 fade per key for a smooth press animation. */
	private final Map<KeyMapping, Float> fade = new HashMap<>();
	private long lastFrame;

	public KeystrokesModule() {
		super("Keystrokes", "Zeigt gedrückte Tasten (WASD, Maus, Leertaste)", true, Anchor.START, 2, Anchor.START, 99);
	}

	@Override
	public int getWidth(boolean editor) {
		return WIDTH + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		int h = KEY * 2 + GAP;
		if (showMouse.isOn()) {
			h += GAP + MOUSE_H;
		}
		if (showSpace.isOn()) {
			h += GAP + SPACE_H;
		}
		return h + PADDING * 2;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0.016F : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
		lastFrame = now;

		Options o = mc().options;
		int left = x + PADDING;
		int top = y + PADDING;
		key(g, left + KEY + GAP, top, KEY, KEY, "W", null, o.keyUp, dt);
		top += KEY + GAP;
		key(g, left, top, KEY, KEY, "A", null, o.keyLeft, dt);
		key(g, left + KEY + GAP, top, KEY, KEY, "S", null, o.keyDown, dt);
		key(g, left + (KEY + GAP) * 2, top, KEY, KEY, "D", null, o.keyRight, dt);
		top += KEY + GAP;
		if (showMouse.isOn()) {
			int half = (WIDTH - GAP) / 2;
			String l = showCps.isOn() ? CpsTracker.left() + " CPS" : null;
			String r = showCps.isOn() ? CpsTracker.right() + " CPS" : null;
			key(g, left, top, half, MOUSE_H, "LMB", l, o.keyAttack, dt);
			key(g, left + half + GAP, top, WIDTH - half - GAP, MOUSE_H, "RMB", r, o.keyUse, dt);
			top += MOUSE_H + GAP;
		}
		if (showSpace.isOn()) {
			float f = animate(o.keyJump, dt);
			Render.rounded(g, left, top, left + WIDTH, top + SPACE_H, background(f));
			int bar = Render.mix(ThemeModule.accent(), 0xFF000000, f);
			g.fill(left + WIDTH / 2 - 12, top + 5, left + WIDTH / 2 + 12, top + 7, bar);
		}
	}

	private float animate(KeyMapping mapping, float dt) {
		float target = mapping.isDown() ? 1.0F : 0.0F;
		float current = fade.getOrDefault(mapping, 0.0F);
		float speed = dt * 12.0F;
		current = target > current ? Math.min(target, current + speed) : Math.max(target, current - speed);
		fade.put(mapping, current);
		return current;
	}

	private int pressed() {
		return pressedColor.is("Theme") ? ThemeModule.accent() : 0xFFFFFFFF;
	}

	private int background(float f) {
		return Render.mix(0x90000000, Render.alpha(pressed(), 0xD8), f);
	}

	private void key(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, String sub, KeyMapping mapping, float dt) {
		float f = animate(mapping, dt);
		Render.rounded(g, x, y, x + w, y + h, background(f));
		boolean dark = f > 0.5F && pressedColor.is("Weiß");
		int color = dark ? 0xFF111111 : 0xFFFFFFFF;
		boolean shadowNow = shadow.isOn() && !dark;
		int tw = font().width(label);
		if (sub == null) {
			g.text(font(), label, x + (w - tw) / 2, y + (h - 8) / 2, color, shadowNow);
			return;
		}
		g.text(font(), label, x + (w - tw) / 2, y + 3, color, shadowNow);
		g.pose().pushMatrix();
		g.pose().translate(x + w / 2.0F, y + 14.0F);
		g.pose().scale(0.75F, 0.75F);
		int sw = font().width(sub);
		g.text(font(), sub, -sw / 2, 0, dark ? 0xFF333333 : 0xFFB8B8C4, false);
		g.pose().popMatrix();
	}
}
