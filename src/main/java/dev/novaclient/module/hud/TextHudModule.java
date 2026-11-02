package dev.novaclient.module.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A single-line HUD element in the form "Label: value". */
public abstract class TextHudModule extends HudModule {
	protected TextHudModule(String name, String description, boolean enabledByDefault, Anchor anchorX, int offsetX, Anchor anchorY, int offsetY) {
		super(name, description, enabledByDefault, anchorX, offsetX, anchorY, offsetY);
	}

	protected abstract String label();

	protected abstract String value();

	private String labelPart() {
		String l = label();
		return l == null || l.isEmpty() ? "" : l + ": ";
	}

	@Override
	public int getWidth(boolean editor) {
		return font().width(labelPart() + value()) + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return font().lineHeight + PADDING * 2 - 1;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		String l = labelPart();
		int tx = x + PADDING;
		int ty = y + PADDING;
		if (!l.isEmpty()) {
			text(g, l, tx, ty, ThemeModule.accent());
			tx += font().width(l);
		}
		text(g, value(), tx, ty, 0xFFFFFFFF);
	}
}
