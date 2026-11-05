package dev.novaclient.module.hud;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A draggable HUD element. The position is stored as a pixel offset from an anchor
 * (left/centre/right, top/centre/bottom), so elements keep their distance to the
 * nearest screen edge when the window size or GUI scale changes.
 */
public abstract class HudModule extends Module {
	public static final int PADDING = 3;
	private static long frame;

	/** Counter of drawn HUD frames (for per-frame caches). */
	public static long hudFrame() {
		return frame;
	}

	/** Called once before the HUD (or the HUD editor) is drawn. */
	public static void nextFrame() {
		frame++;
	}

	public enum Anchor {
		START, CENTER, END;

		static Anchor parse(String s) {
			for (Anchor a : values()) {
				if (a.name().equalsIgnoreCase(s)) {
					return a;
				}
			}
			return START;
		}
	}

	private final Anchor defaultAnchorX;
	private final Anchor defaultAnchorY;
	private final int defaultOffsetX;
	private final int defaultOffsetY;
	private Anchor anchorX;
	private Anchor anchorY;
	private int offsetX;
	private int offsetY;

	public final BoolSetting background = add(new BoolSetting("Hintergrund", true));
	public final BoolSetting shadow = add(new BoolSetting("Textschatten", true));

	protected HudModule(String name, String description, boolean enabledByDefault, Anchor anchorX, int offsetX, Anchor anchorY, int offsetY) {
		super(name, description, Category.HUD, enabledByDefault);
		this.anchorX = this.defaultAnchorX = anchorX;
		this.anchorY = this.defaultAnchorY = anchorY;
		this.offsetX = this.defaultOffsetX = offsetX;
		this.offsetY = this.defaultOffsetY = offsetY;
	}

	@Override
	public void resetToDefaults() {
		super.resetToDefaults();
		resetPosition();
	}

	public void resetPosition() {
		anchorX = defaultAnchorX;
		anchorY = defaultAnchorY;
		offsetX = defaultOffsetX;
		offsetY = defaultOffsetY;
	}

	protected static Font font() {
		return mc().font;
	}

	/** Width of the element including padding. */
	public abstract int getWidth(boolean editor);

	/** Height of the element including padding. */
	public abstract int getHeight(boolean editor);

	/** Draws the content; (x, y) is the top-left corner including padding. */
	protected abstract void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor);

	private static int resolve(Anchor anchor, int offset, int screen, int size) {
		int pos = switch (anchor) {
			case START -> offset;
			case CENTER -> (screen - size) / 2 + offset;
			case END -> screen - size - offset;
		};
		return Math.max(0, Math.min(pos, Math.max(0, screen - size)));
	}

	public int getScreenX(int screenWidth, boolean editor) {
		return resolve(anchorX, offsetX, screenWidth, getWidth(editor));
	}

	public int getScreenY(int screenHeight, boolean editor) {
		return resolve(anchorY, offsetY, screenHeight, getHeight(editor));
	}

	/** Moves the element so that its top-left corner is at (x, y); picks the closest anchor. */
	public void setScreenPos(int x, int y, int screenWidth, int screenHeight, boolean editor) {
		int w = getWidth(editor);
		int h = getHeight(editor);
		x = Math.max(0, Math.min(x, Math.max(0, screenWidth - w)));
		y = Math.max(0, Math.min(y, Math.max(0, screenHeight - h)));
		anchorX = pick(x + w / 2.0, screenWidth);
		anchorY = pick(y + h / 2.0, screenHeight);
		offsetX = offsetFor(anchorX, x, screenWidth, w);
		offsetY = offsetFor(anchorY, y, screenHeight, h);
	}

	private static Anchor pick(double center, int screen) {
		if (center < screen / 3.0) {
			return Anchor.START;
		}
		if (center > screen * 2 / 3.0) {
			return Anchor.END;
		}
		return Anchor.CENTER;
	}

	private static int offsetFor(Anchor anchor, int pos, int screen, int size) {
		return switch (anchor) {
			case START -> pos;
			case CENTER -> pos - (screen - size) / 2;
			case END -> screen - size - pos;
		};
	}

	public Anchor getAnchorX() {
		return anchorX;
	}

	public Anchor getAnchorY() {
		return anchorY;
	}

	public int getOffsetX() {
		return offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public void setPosition(String anchorX, int offsetX, String anchorY, int offsetY) {
		this.anchorX = Anchor.parse(anchorX);
		this.anchorY = Anchor.parse(anchorY);
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public final void render(GuiGraphicsExtractor g, int screenWidth, int screenHeight, boolean editor) {
		int w = getWidth(editor);
		int h = getHeight(editor);
		if (w <= 0 || h <= 0) {
			return;
		}
		int x = getScreenX(screenWidth, editor);
		int y = getScreenY(screenHeight, editor);
		if (background.isOn()) {
			g.fill(x, y, x + w, y + h, 0x90000000);
		}
		renderContent(g, x, y, editor);
	}

	protected void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
		g.text(font(), s, x, y, color, shadow.isOn());
	}
}
