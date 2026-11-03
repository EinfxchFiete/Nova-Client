package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.cosmetics.Badges;
import dev.novaclient.cosmetics.CosmeticRegistry;
import dev.novaclient.emote.Emotes;
import dev.novaclient.module.Module;
import dev.novaclient.module.cosmetics.CapeModule;
import dev.novaclient.module.cosmetics.EmotesModule;
import dev.novaclient.module.cosmetics.HeadCosmeticModule;
import dev.novaclient.module.cosmetics.WingsModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.module.utility.NetworkModule;
import dev.novaclient.network.NovaNetwork;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.util.Render;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Wardrobe: 3D preview of your player on the left, cosmetics to pick on the right. */
public class CosmeticsScreen extends Screen {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int TILE_H = 34;
	private static final int GAP = 5;
	private static final int SWATCH = 14;
	private static final String[] TABS = {"Capes", "Kopf", "Flügel", "Emotes", "Effekte"};
	private static final String[] EFFECTS = {"Partikel-Spur", "Aura", "Partikel-Ring", "Treffer-Partikel", "Begleiter", "Nametags"};

	private static int lastTab;

	private final @Nullable Screen parent;
	private int tab = lastTab;
	private float yaw = 200;
	private boolean draggingPreview;
	private long lastInteraction = System.currentTimeMillis();
	private double scroll;

	private int left;
	private int top;
	private int winW;
	private int winH;

	/** One clickable entry of the current tab. */
	private record Tile(String label, String sub, boolean selected, Runnable action, @Nullable String capeId) {
	}

	public CosmeticsScreen(@Nullable Screen parent) {
		super(Component.literal("Cosmetics"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		NovaClient.config().save();
		NovaNetwork.requestSync();
		minecraft.gui.setScreen(parent);
	}

	private void layout() {
		winW = Math.min(470, width - 20);
		winH = Math.min(292, height - 20);
		left = (width - winW) / 2;
		top = (height - winH) / 2;
	}

	private int previewW() {
		return Math.max(110, winW * 32 / 100);
	}

	private int gridX() {
		return left + previewW() + 18;
	}

	private int gridW() {
		return left + winW - 12 - gridX();
	}

	private int gridTop() {
		return top + 34;
	}

	private int gridBottom() {
		return top + winH - (hasColors() ? 52 : 26);
	}

	private int columns() {
		return gridW() >= 260 ? 3 : 2;
	}

	private int tileW() {
		return (gridW() - GAP * (columns() - 1)) / columns();
	}

	private boolean hasColors() {
		return tab == 1 || tab == 2;
	}

	private int[][] tabBounds() {
		int[][] out = new int[TABS.length][2];
		int x = gridX();
		for (int i = 0; i < TABS.length; i++) {
			int w = font.width(TABS[i]) + 12;
			out[i][0] = x;
			out[i][1] = x + w;
			x += w + 3;
		}
		return out;
	}

	// ------------------------------------------------------------------ tiles per tab

	private List<Tile> tiles() {
		List<Tile> out = new ArrayList<>();
		switch (tab) {
			case 0 -> {
				CapeModule cape = CapeModule.get();
				out.add(new Tile("Kein Cape", "", !cape.isEnabled(), () -> cape.setEnabled(false), null));
				for (String design : cape.design.getModes()) {
					boolean animated = design.contains("animiert");
					String label = design.replace(" (animiert)", "");
					String sub = animated ? "animiert" : design.equals("Wechselnd") ? "alle 2 s neu" : "";
					String id = design.equals("Wechselnd") ? "nova" : CapeModule.idFor(design);
					out.add(new Tile(label, sub, cape.isEnabled() && cape.design.get().equals(design), () -> {
						cape.design.set(design);
						cape.setEnabled(true);
					}, id));
				}
			}
			case 1 -> {
				HeadCosmeticModule head = HeadCosmeticModule.get();
				out.add(new Tile("Nichts", "", !head.isEnabled(), () -> head.setEnabled(false), null));
				for (String name : CosmeticRegistry.HEADS.values()) {
					out.add(new Tile(name, "3D", head.isEnabled() && head.type.get().equals(name), () -> {
						head.type.set(name);
						head.setEnabled(true);
					}, null));
				}
			}
			case 2 -> {
				WingsModule wings = WingsModule.get();
				out.add(new Tile("Keine", "", !wings.isEnabled(), () -> wings.setEnabled(false), null));
				for (String name : CosmeticRegistry.WINGS.values()) {
					out.add(new Tile(name, "animiert", wings.isEnabled() && wings.type.get().equals(name), () -> {
						wings.type.set(name);
						wings.setEnabled(true);
					}, null));
				}
			}
			case 3 -> {
				Emotes.Active active = Emotes.localActive();
				for (Map.Entry<String, Emotes.Info> e : Emotes.ALL.entrySet()) {
					out.add(new Tile(e.getValue().name(), "abspielen", active != null && active.id().equals(e.getKey()),
						() -> Emotes.play(e.getKey()), null));
				}
			}
			default -> {
				for (String name : EFFECTS) {
					Module m = NovaClient.modules().byName(name);
					if (m != null) {
						out.add(new Tile(name, m.isEnabled() ? "AN" : "AUS", m.isEnabled(), m::toggle, null));
					}
				}
			}
		}
		return out;
	}

	private @Nullable ModeSetting colorSetting() {
		return tab == 1 ? HeadCosmeticModule.get().color : tab == 2 ? WingsModule.get().color : null;
	}

	private int tileX(int i) {
		return gridX() + (i % columns()) * (tileW() + GAP);
	}

	private int tileY(int i) {
		return gridTop() + (i / columns()) * (TILE_H + GAP) - (int) scroll;
	}

	private double maxScroll() {
		int rows = (tiles().size() + columns() - 1) / columns();
		return Math.max(0, rows * (TILE_H + GAP) - (gridBottom() - gridTop()));
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		layout();
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
		int accent = ThemeModule.accent();
		Render.rounded(g, left, top, left + winW, top + winH, Render.alpha(0xFF0E0E14, ThemeModule.menuAlpha()));
		Render.roundedOutline(g, left, top, left + winW, top + winH, Render.alpha(accent, 0x70));

		renderPreview(g, mouseX, mouseY, partialTick, accent);

		// tabs
		g.text(font, "COSMETICS", gridX(), top + 4, 0xFF6C6C7E, false);
		int[][] tabs = tabBounds();
		for (int i = 0; i < TABS.length; i++) {
			boolean sel = i == tab;
			boolean hover = Render.inside(mouseX, mouseY, tabs[i][0], top + 14, tabs[i][1], top + 28);
			if (sel) {
				Render.rounded(g, tabs[i][0], top + 14, tabs[i][1], top + 28, Render.alpha(accent, 0x55));
			} else if (hover) {
				Render.rounded(g, tabs[i][0], top + 14, tabs[i][1], top + 28, 0x22FFFFFF);
			}
			g.text(font, TABS[i], tabs[i][0] + 6, top + 17, sel ? 0xFFFFFFFF : 0xFF9A9AAE, false);
		}

		// tiles
		List<Tile> tiles = tiles();
		g.enableScissor(gridX() - 2, gridTop() - 2, gridX() + gridW() + 2, gridBottom() + 2);
		int tw = tileW();
		for (int i = 0; i < tiles.size(); i++) {
			Tile t = tiles.get(i);
			int x = tileX(i);
			int y = tileY(i);
			if (y + TILE_H < gridTop() - 2 || y > gridBottom() + 2) {
				continue;
			}
			boolean hover = mouseY >= gridTop() && mouseY < gridBottom() && Render.inside(mouseX, mouseY, x, y, x + tw, y + TILE_H);
			int bg = hover ? 0xFF22222D : 0xFF181820;
			if (t.selected()) {
				bg = Render.mix(bg, accent, 0.2F);
			}
			Render.rounded(g, x, y, x + tw, y + TILE_H, bg);
			if (t.selected()) {
				Render.roundedOutline(g, x, y, x + tw, y + TILE_H, Render.alpha(accent, 0xC0));
			}
			int textX = x + 7;
			if (t.capeId() != null) {
				// cape front (10x16 at 1,1 of the cape texture), animated capes show their current frame
				g.blit(RenderPipelines.GUI_TEXTURED, CapeModule.texturePath(t.capeId()), x + 5, y + 4, 1, 1, 16, 26, 10, 16, 64, 32);
				textX = x + 26;
			}
			g.text(font, font.plainSubstrByWidth(t.label(), x + tw - 6 - textX), textX, y + 8, 0xFFFFFFFF, false);
			if (!t.sub().isEmpty()) {
				g.text(font, t.sub(), textX, y + 19, t.selected() ? accent : 0xFF7C7C8E, false);
			}
		}
		g.disableScissor();
		if (tiles.isEmpty()) {
			g.centeredText(font, "Nichts verfügbar", gridX() + gridW() / 2, gridTop() + 20, 0xFF9A9AAE);
		}

		// colours
		ModeSetting colors = colorSetting();
		if (colors != null) {
			int cy = top + winH - 46;
			g.text(font, "Farbe:", gridX(), cy + 3, 0xFF9A9AAE, false);
			List<String> names = colors.getModes();
			for (int i = 0; i < names.size(); i++) {
				int[] p = swatchPos(i);
				String n = names.get(i);
				int c = CosmeticRegistry.tint(n, System.currentTimeMillis() / 1000F);
				boolean sel = colors.get().equals(n);
				boolean hover = Render.inside(mouseX, mouseY, p[0], p[1], p[0] + SWATCH, p[1] + SWATCH);
				if (sel || hover) {
					Render.rounded(g, p[0] - 2, p[1] - 2, p[0] + SWATCH + 2, p[1] + SWATCH + 2, sel ? 0xFFFFFFFF : 0x80FFFFFF);
				}
				if (n.equals("Regenbogen")) {
					int[] rainbow = {0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA};
					int sw = SWATCH / rainbow.length + 1;
					for (int k = 0; k < rainbow.length; k++) {
						g.fill(p[0] + k * SWATCH / rainbow.length, p[1], Math.min(p[0] + SWATCH, p[0] + k * SWATCH / rainbow.length + sw), p[1] + SWATCH, rainbow[k]);
					}
				} else {
					Render.rounded(g, p[0], p[1], p[0] + SWATCH, p[1] + SWATCH, n.equals("Normal") ? 0xFF555566 : c);
				}
				if (n.equals("Normal")) {
					g.text(font, "N", p[0] + 4, p[1] + 3, 0xFFFFFFFF, false);
				}
				if (hover) {
					g.setTooltipForNextFrame(font, Component.literal(n), mouseX, mouseY);
				}
			}
		}

		// footer: network status and hints
		String status = networkStatus();
		g.text(font, status, gridX(), top + winH - 16, 0xFF6C6C7E, false);
		if (tab == 3) {
			String hint = "Emote-Rad im Spiel: Taste " + EmotesModule.get().wheelKey.display();
			g.text(font, hint, gridX(), top + winH - 28, 0xFF9A9AAE, false);
		}
	}

	private String networkStatus() {
		NetworkModule net = NetworkModule.get();
		if (net == null || !net.isEnabled() || net.serverUrl().isEmpty()) {
			return "Nova-Netzwerk: aus – nur du siehst deine Cosmetics";
		}
		if (NovaNetwork.isConnected()) {
			return "Nova-Netzwerk: verbunden · " + NovaNetwork.knownPlayers() + " Nova-Spieler hier";
		}
		String err = NovaNetwork.lastError();
		return "Nova-Netzwerk: verbinde…" + (err.isEmpty() ? "" : " (" + err + ")");
	}

	private int[] swatchPos(int i) {
		int x = gridX() + font.width("Farbe:") + 8 + i * (SWATCH + 5);
		return new int[] {x, top + winH - 48};
	}

	private void renderPreview(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, int accent) {
		int x0 = left + 8;
		int y0 = top + 8;
		int x1 = left + 8 + previewW();
		int y1 = top + winH - 8;
		g.fillGradient(x0, y0, x1, y1, 0xFF1A1830, 0xFF0F0F16);
		Render.roundedOutline(g, x0, y0, x1, y1, 0x30FFFFFF);
		LocalPlayer p = minecraft.player;
		if (p == null) {
			return;
		}
		if (!draggingPreview && System.currentTimeMillis() - lastInteraction > 2500) {
			yaw += partialTick * 0.0F + 0.6F;
		}
		EntityRenderer<? super LocalPlayer, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(p);
		EntityRenderState state = renderer.createRenderState(p, 1.0F);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		state.nameTag = null;
		state.scoreText = null;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = yaw;
			living.yRot = 0;
			living.xRot = 0;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1.0F;
		}
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf camera = new Quaternionf().rotateX(-0.12F);
		rotation.mul(camera);
		int size = Math.min((y1 - y0) / 3, (x1 - x0) / 2);
		Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.15F, 0.0F);
		g.entity(state, size, translation, rotation, camera, x0, y0 + 14, x1, y1 - 18);

		Component name = Badges.decorate(p.getUUID(), true, p.getName(), false);
		g.centeredText(font, name, (x0 + x1) / 2, y0 + 6, 0xFFFFFFFF);
		g.centeredText(font, "Ziehen zum Drehen", (x0 + x1) / 2, y1 - 12, 0xFF6C6C7E);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		layout();
		double mx = event.x();
		double my = event.y();
		lastInteraction = System.currentTimeMillis();
		if (event.button() != LEFT) {
			return super.mouseClicked(event, doubleClick);
		}
		if (Render.inside(mx, my, left + 8, top + 8, left + 8 + previewW(), top + winH - 8)) {
			draggingPreview = true;
			return true;
		}
		int[][] tabs = tabBounds();
		for (int i = 0; i < tabs.length; i++) {
			if (Render.inside(mx, my, tabs[i][0], top + 14, tabs[i][1], top + 28)) {
				tab = i;
				lastTab = i;
				scroll = 0;
				return true;
			}
		}
		ModeSetting colors = colorSetting();
		if (colors != null) {
			List<String> names = colors.getModes();
			for (int i = 0; i < names.size(); i++) {
				int[] p = swatchPos(i);
				if (Render.inside(mx, my, p[0], p[1], p[0] + SWATCH, p[1] + SWATCH)) {
					colors.set(names.get(i));
					NovaNetwork.requestSync();
					return true;
				}
			}
		}
		if (my >= gridTop() && my < gridBottom()) {
			List<Tile> tiles = tiles();
			for (int i = 0; i < tiles.size(); i++) {
				int x = tileX(i);
				int y = tileY(i);
				if (Render.inside(mx, my, x, y, x + tileW(), y + TILE_H)) {
					tiles.get(i).action().run();
					NovaNetwork.requestSync();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingPreview) {
			yaw -= (float) dx * 2.5F;
			lastInteraction = System.currentTimeMillis();
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingPreview = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		layout();
		scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 20));
		return true;
	}

	// ------------------------------------------------------------------ test helpers

	public int[] tabCenter(int i) {
		layout();
		int[][] t = tabBounds();
		return new int[] {(t[i][0] + t[i][1]) / 2, top + 21};
	}

	public int @Nullable [] tileCenter(String label) {
		layout();
		List<Tile> tiles = tiles();
		for (int i = 0; i < tiles.size(); i++) {
			if (tiles.get(i).label().equals(label)) {
				return new int[] {tileX(i) + tileW() / 2, tileY(i) + TILE_H / 2};
			}
		}
		return null;
	}

	public int[] swatchCenter(String color) {
		layout();
		ModeSetting c = colorSetting();
		int i = c == null ? 0 : c.getModes().indexOf(color);
		int[] p = swatchPos(i);
		return new int[] {p[0] + SWATCH / 2, p[1] + SWATCH / 2};
	}

	/** Scrolls the grid so the tile with this label is visible (used by tests). */
	public void scrollTo(String label) {
		layout();
		List<Tile> tiles = tiles();
		for (int i = 0; i < tiles.size(); i++) {
			if (tiles.get(i).label().equals(label)) {
				int y = gridTop() + (i / columns()) * (TILE_H + GAP);
				scroll = Math.max(0, Math.min(maxScroll(), y - gridTop()));
				return;
			}
		}
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
		this.lastInteraction = System.currentTimeMillis() + 60_000L;
	}
}
