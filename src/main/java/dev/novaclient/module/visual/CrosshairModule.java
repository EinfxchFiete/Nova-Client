package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Replaces the vanilla crosshair with a configurable one. */
public class CrosshairModule extends Module {
	private static CrosshairModule instance;

	public final ModeSetting style = add(new ModeSetting("Stil", "Kreuz", "Kreuz", "Kreuz + Punkt", "Punkt", "Kreis", "T-Form", "Quadrat"));
	public final ModeSetting color = add(new ModeSetting("Farbe", "Weiß", "Weiß", "Theme", "Rot", "Grün", "Blau", "Gelb", "Pink", "Cyan", "Schwarz"));
	public final NumberSetting size = add(new NumberSetting("Größe", 4, 1, 12, 1));
	public final NumberSetting gap = add(new NumberSetting("Abstand", 2, 0, 8, 1));
	public final NumberSetting thickness = add(new NumberSetting("Dicke", 1, 1, 4, 1));
	public final NumberSetting opacity = add(new NumberSetting("Deckkraft", 1.0, 0.2, 1.0, 0.05));
	public final BoolSetting outline = add(new BoolSetting("Umrandung", true));

	public CrosshairModule() {
		super("Custom Crosshair", "Eigenes Fadenkreuz mit Stil, Farbe und Größe", Category.VISUAL, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	public static CrosshairModule get() {
		return instance;
	}

	private int colorValue() {
		int rgb = switch (color.get()) {
			case "Theme" -> ThemeModule.accent() & 0xFFFFFF;
			case "Rot" -> 0xFF4040;
			case "Grün" -> 0x40FF60;
			case "Blau" -> 0x4080FF;
			case "Gelb" -> 0xFFE040;
			case "Pink" -> 0xFF60D0;
			case "Cyan" -> 0x40E8FF;
			case "Schwarz" -> 0x000000;
			default -> 0xFFFFFF;
		};
		return ((int) Math.round(opacity.get() * 255) << 24) | rgb;
	}

	public void render(GuiGraphicsExtractor g) {
		int cx = g.guiWidth() / 2;
		int cy = g.guiHeight() / 2;
		int main = colorValue();
		if (outline.isOn()) {
			int outlineColor = ((int) Math.round(opacity.get() * 200) << 24) | (color.is("Schwarz") ? 0xFFFFFF : 0x000000);
			draw(g, cx, cy, outlineColor, 1);
		}
		draw(g, cx, cy, main, 0);
	}

	/** Draws the shape; grow > 0 draws it 1px bigger in every direction (for the outline). */
	private void draw(GuiGraphicsExtractor g, int cx, int cy, int c, int grow) {
		int s = size.getInt();
		int gp = gap.getInt();
		int t = thickness.getInt();
		int lo = -(t / 2) - grow;
		int hi = lo + t + 2 * grow;
		switch (style.get()) {
			case "Punkt" -> dot(g, cx, cy, t, grow, c);
			case "Kreis" -> circle(g, cx, cy, gp + s, t, grow, c);
			case "Quadrat" -> {
				int r = gp + s;
				g.fill(cx - r - grow, cy - r - grow, cx + r + 1 + grow, cy - r + t + grow, c);
				g.fill(cx - r - grow, cy + r + 1 - t - grow, cx + r + 1 + grow, cy + r + 1 + grow, c);
				g.fill(cx - r - grow, cy - r + t - grow, cx - r + t + grow, cy + r + 1 - t + grow, c);
				g.fill(cx + r + 1 - t - grow, cy - r + t - grow, cx + r + 1 + grow, cy + r + 1 - t + grow, c);
			}
			default -> {
				// left, right, bottom arms
				g.fill(cx - gp - s - grow, cy + lo, cx - gp + grow, cy + hi, c);
				g.fill(cx + 1 + gp - grow, cy + lo, cx + 1 + gp + s + grow, cy + hi, c);
				g.fill(cx + lo, cy + 1 + gp - grow, cx + hi, cy + 1 + gp + s + grow, c);
				if (!style.is("T-Form")) {
					g.fill(cx + lo, cy - gp - s - grow, cx + hi, cy - gp + grow, c);
				}
				if (style.is("Kreuz + Punkt")) {
					dot(g, cx, cy, t, grow, c);
				}
			}
		}
	}

	private static void dot(GuiGraphicsExtractor g, int cx, int cy, int t, int grow, int c) {
		int lo = -(t / 2) - grow;
		int hi = lo + Math.max(t, 1) + 2 * grow;
		g.fill(cx + lo, cy + lo, cx + hi, cy + hi, c);
	}

	private static void circle(GuiGraphicsExtractor g, int cx, int cy, int radius, int t, int grow, int c) {
		double outer = radius + 0.5 + grow;
		double inner = Math.max(0, radius + 0.5 - t - grow);
		int r = (int) Math.ceil(outer);
		for (int y = -r; y <= r; y++) {
			int start = Integer.MIN_VALUE;
			for (int x = -r; x <= r + 1; x++) {
				double d = Math.sqrt(x * x + y * y);
				boolean in = x <= r && d <= outer && d >= inner;
				if (in && start == Integer.MIN_VALUE) {
					start = x;
				} else if (!in && start != Integer.MIN_VALUE) {
					g.fill(cx + start, cy + y, cx + x, cy + y + 1, c);
					start = Integer.MIN_VALUE;
				}
			}
		}
	}
}
