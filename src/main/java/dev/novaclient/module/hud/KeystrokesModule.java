package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import dev.novaclient.util.CpsTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeystrokesModule extends HudModule {
	private static final int KEY = 22;
	private static final int GAP = 2;
	private static final int WIDTH = KEY * 3 + GAP * 2;

	public final BoolSetting showMouse = add(new BoolSetting("Maustasten", true));
	public final BoolSetting showSpace = add(new BoolSetting("Leertaste", true));

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
			h += GAP + KEY;
		}
		if (showSpace.isOn()) {
			h += GAP + 12;
		}
		return h + PADDING * 2;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		Options o = mc().options;
		int left = x + PADDING;
		int top = y + PADDING;
		key(g, left + KEY + GAP, top, KEY, KEY, "W", null, o.keyUp);
		top += KEY + GAP;
		key(g, left, top, KEY, KEY, "A", null, o.keyLeft);
		key(g, left + KEY + GAP, top, KEY, KEY, "S", null, o.keyDown);
		key(g, left + (KEY + GAP) * 2, top, KEY, KEY, "D", null, o.keyRight);
		top += KEY + GAP;
		if (showMouse.isOn()) {
			int half = (WIDTH - GAP) / 2;
			key(g, left, top, half, KEY, "LMB", CpsTracker.left() + " CPS", o.keyAttack);
			key(g, left + half + GAP, top, WIDTH - half - GAP, KEY, "RMB", CpsTracker.right() + " CPS", o.keyUse);
			top += KEY + GAP;
		}
		if (showSpace.isOn()) {
			boolean down = o.keyJump.isDown();
			g.fill(left, top, left + WIDTH, top + 12, down ? 0xC0FFFFFF : 0x80000000);
			int barColor = down ? 0xFF000000 : ThemeModule.accent();
			g.fill(left + WIDTH / 2 - 12, top + 5, left + WIDTH / 2 + 12, top + 7, barColor);
		}
	}

	private void key(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, String sub, KeyMapping mapping) {
		boolean down = mapping.isDown();
		g.fill(x, y, x + w, y + h, down ? 0xC0FFFFFF : 0x80000000);
		int color = down ? 0xFF000000 : 0xFFFFFFFF;
		boolean shadowNow = shadow.isOn() && !down;
		if (sub == null) {
			int tw = font().width(label);
			g.text(font(), label, x + (w - tw) / 2, y + (h - 8) / 2, color, shadowNow);
		} else {
			int tw = font().width(label);
			g.text(font(), label, x + (w - tw) / 2, y + 2, color, shadowNow);
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f, y + 13);
			g.pose().scale(0.6f, 0.6f);
			int sw = font().width(sub);
			g.text(font(), sub, -sw / 2, 0, down ? 0xFF333333 : 0xFFAAAAAA, false);
			g.pose().popMatrix();
		}
	}
}
