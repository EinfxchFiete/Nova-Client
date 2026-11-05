package dev.novaclient.module.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A single-line HUD element in the form "Label: value". */
public abstract class TextHudModule extends HudModule {
	protected TextHudModule(String name, String description, boolean enabledByDefault, Anchor anchorX, int offsetX, Anchor anchorY, int offsetY) {
		super(name, description, enabledByDefault, anchorX, offsetX, anchorY, offsetY);
	}

	protected abstract String label();

	protected abstract String value();

	// label, value and width are built once per frame (the HUD asks for the width several times per frame)
	private long cachedFrame = -1;
	private String cachedLabel = "";
	private String cachedValue = "";
	private int cachedWidth;

	private void refresh() {
		if (cachedFrame == HudModule.hudFrame()) {
			return;
		}
		String l = label();
		cachedLabel = l == null || l.isEmpty() ? "" : l + ": ";
		String v = value();
		cachedValue = v == null ? "" : v;
		cachedWidth = font().width(cachedLabel) + font().width(cachedValue);
		cachedFrame = HudModule.hudFrame();
	}

	private String labelPart() {
		refresh();
		return cachedLabel;
	}

	@Override
	public int getWidth(boolean editor) {
		refresh();
		return cachedWidth + PADDING * 2;
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
		text(g, cachedValue, tx, ty, 0xFFFFFFFF);
	}
}
