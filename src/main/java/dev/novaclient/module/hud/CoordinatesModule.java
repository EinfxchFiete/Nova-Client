package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

public class CoordinatesModule extends HudModule {
	public final BoolSetting showDirection = add(new BoolSetting("Richtung anzeigen", true));

	public CoordinatesModule() {
		super("Koordinaten", "XYZ-Position und Blickrichtung", true, Anchor.START, 2, Anchor.START, 50);
	}

	private String[] lines() {
		LocalPlayer p = mc().player;
		String x = p == null ? "0" : Integer.toString(p.blockPosition().getX());
		String y = p == null ? "64" : Integer.toString(p.blockPosition().getY());
		String z = p == null ? "0" : Integer.toString(p.blockPosition().getZ());
		String dir = p == null ? "Norden (-Z)" : direction(p.getYRot());
		if (showDirection.isOn()) {
			return new String[] {"X: " + x, "Y: " + y, "Z: " + z, dir};
		}
		return new String[] {"X: " + x, "Y: " + y, "Z: " + z};
	}

	private static String direction(float yaw) {
		float y = ((yaw % 360) + 360) % 360;
		int idx = Math.round(y / 45f) & 7;
		return switch (idx) {
			case 0 -> "Süden (+Z)";
			case 1 -> "Südwesten";
			case 2 -> "Westen (-X)";
			case 3 -> "Nordwesten";
			case 4 -> "Norden (-Z)";
			case 5 -> "Nordosten";
			case 6 -> "Osten (+X)";
			default -> "Südosten";
		};
	}

	@Override
	public int getWidth(boolean editor) {
		int w = 0;
		for (String s : lines()) {
			w = Math.max(w, font().width(s));
		}
		return w + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return lines().length * (font().lineHeight + 1) + PADDING * 2 - 1;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		String[] lines = lines();
		int ty = y + PADDING;
		for (int i = 0; i < lines.length; i++) {
			String s = lines[i];
			int tx = x + PADDING;
			if (i < 3) {
				String label = s.substring(0, 3);
				text(g, label, tx, ty, ThemeModule.accent());
				text(g, s.substring(3), tx + font().width(label), ty, 0xFFFFFFFF);
			} else {
				text(g, s, tx, ty, 0xFFAAAAAA);
			}
			ty += font().lineHeight + 1;
		}
	}
}
