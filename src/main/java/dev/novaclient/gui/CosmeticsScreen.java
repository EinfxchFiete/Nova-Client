package dev.novaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.cosmetics.Badges;
import dev.novaclient.cosmetics.CosmeticRegistry;
import dev.novaclient.emote.Emotes;
import dev.novaclient.module.Module;
import dev.novaclient.module.cosmetics.CapeModule;
import dev.novaclient.module.cosmetics.CompanionModule;
import dev.novaclient.module.cosmetics.EmotesModule;
import dev.novaclient.module.cosmetics.ExtrasModule;
import dev.novaclient.module.cosmetics.HeadCosmeticModule;
import dev.novaclient.module.cosmetics.MinimeeModule;
import dev.novaclient.module.cosmetics.WingsModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.module.utility.NetworkModule;
import dev.novaclient.network.NovaNetwork;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.setting.Setting;
import dev.novaclient.setting.TextSetting;
import dev.novaclient.util.Render;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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

/**
 * The one and only cosmetics menu: 3D preview of your player on the left, everything you can wear or show on the right
 * (capes, head, wings, minimee, companion, effects, emotes) including all their options.
 */
public class CosmeticsScreen extends Screen {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;
	private static final int TILE_H = 34;
	private static final int ROW_H = 18;
	private static final int HEADER_H = 14;
	private static final int GAP = 5;
	private static final int SWATCH = 14;
	/** Automatic rotation of the preview in degrees per second. */
	private static final float SPIN_SPEED = 24F;
	/** Degrees per dragged GUI pixel. */
	private static final float DRAG_SPEED = 0.9F;
	private static final String[] TABS = {"Capes", "Kopf", "Flügel", "Minimee", "Begleiter", "Effekte", "Emotes", "Extras"};
	private static final String[] EFFECTS = {"Partikel-Spur", "Partikel-Ring", "Treffer-Partikel", "Kill-Effekt", "Nametags"};

	private static int lastTab;

	private final @Nullable Screen parent;
	private int tab = lastTab;
	private float yaw = 200;
	private boolean draggingPreview;
	private long lastInteraction = System.currentTimeMillis();
	private long lastFrame = System.nanoTime();
	private double scroll;
	private @Nullable TextSetting editing;

	private int left;
	private int top;
	private int winW;
	private int winH;

	// ------------------------------------------------------------------ content model

	private enum Kind { TILE, HEADER, BOOL, MODE, NUMBER, TEXT, COLORS, HINT }

	/** One entry on the right side. Tiles flow in a grid, everything else takes a full row. */
	private record Item(Kind kind, String label, String sub, boolean selected, @Nullable Runnable action, @Nullable String capeId,
		@Nullable Setting<?> setting, @Nullable Module module) {
		static Item tile(String label, String sub, boolean selected, Runnable action) {
			return new Item(Kind.TILE, label, sub, selected, action, null, null, null);
		}

		static Item cape(String label, String sub, boolean selected, Runnable action, String capeId) {
			return new Item(Kind.TILE, label, sub, selected, action, capeId, null, null);
		}

		static Item header(String label, @Nullable Module toggle) {
			return new Item(Kind.HEADER, label, "", false, null, null, null, toggle);
		}

		static Item hint(String text) {
			return new Item(Kind.HINT, text, "", false, null, null, null, null);
		}

		static Item setting(Setting<?> s, String label) {
			Kind k = s instanceof BoolSetting ? Kind.BOOL : s instanceof ModeSetting ? Kind.MODE
				: s instanceof NumberSetting ? Kind.NUMBER : s instanceof TextSetting ? Kind.TEXT : Kind.HINT;
			return new Item(k, label, "", false, null, null, s, null);
		}

		static Item colors(ModeSetting s, String label) {
			return new Item(Kind.COLORS, label, "", false, null, null, s, null);
		}
	}

	/** An item with its position on screen (y without scrolling). */
	private record Placed(Item item, int x, int y, int w, int h) {
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

	/** Typing (minimee name) needs SDL text input, see ClickGuiScreen. */
	@Override
	public void added() {
		super.added();
		minecraft.textInputManager().startTextInput(this);
	}

	@Override
	public void removed() {
		minecraft.textInputManager().stopTextInput(this);
		super.removed();
	}

	@Override
	public void tick() {
		super.tick();
		minecraft.textInputManager().startTextInput(this);
	}

	private void layout() {
		winW = Math.min(540, width - 20);
		winH = Math.min(310, height - 20);
		left = (width - winW) / 2;
		top = (height - winH) / 2;
	}

	private int previewW() {
		return Math.max(110, winW * 28 / 100);
	}

	private int gridX() {
		return left + previewW() + 18;
	}

	private int gridW() {
		return left + winW - 12 - gridX();
	}

	/** Tab positions: {x0, x1, y}; the tabs wrap into a second row if the window is narrow. */
	private int[][] tabBounds() {
		int[][] out = new int[TABS.length][3];
		int x = gridX();
		int y = top + 14;
		for (int i = 0; i < TABS.length; i++) {
			int w = font.width(TABS[i]) + 10;
			if (x + w > gridX() + gridW() && x > gridX()) {
				x = gridX();
				y += 17;
			}
			out[i][0] = x;
			out[i][1] = x + w;
			out[i][2] = y;
			x += w + 3;
		}
		return out;
	}

	private int gridTop() {
		int[][] t = tabBounds();
		return t[t.length - 1][2] + 20;
	}

	private int gridBottom() {
		return top + winH - 22;
	}

	private int columns() {
		return gridW() >= 270 ? 3 : 2;
	}

	private int tileW() {
		return (gridW() - GAP * (columns() - 1)) / columns();
	}

	private List<Item> items() {
		List<Item> out = new ArrayList<>();
		switch (tab) {
			case 0 -> {
				CapeModule cape = CapeModule.get();
				out.add(Item.tile("Kein Cape", "", !cape.isEnabled(), () -> cape.setEnabled(false)));
				out.add(Item.header("Capes", null));
				for (String design : cape.design.getModes()) {
					if (design.contains("animiert")) {
						continue;
					}
					String sub = design.equals("Wechselnd") ? "alle 2 s neu" : "";
					String id = design.equals("Wechselnd") ? "nova" : CapeModule.idFor(design);
					out.add(Item.cape(design, sub, cape.isEnabled() && cape.design.get().equals(design), () -> {
						cape.design.set(design);
						cape.setEnabled(true);
					}, id));
				}
				out.add(Item.header("Animierte Capes", null));
				for (String design : cape.design.getModes()) {
					if (!design.contains("animiert")) {
						continue;
					}
					out.add(Item.cape(design.replace(" (animiert)", ""), "animiert", cape.isEnabled() && cape.design.get().equals(design), () -> {
						cape.design.set(design);
						cape.setEnabled(true);
					}, CapeModule.idFor(design)));
				}
				out.add(Item.header("Optionen", null));
				out.add(Item.setting(cape.elytra, cape.elytra.getName()));
			}
			case 1 -> {
				HeadCosmeticModule head = HeadCosmeticModule.get();
				// colours first, so they are always visible without scrolling
				out.add(Item.colors(head.color, head.type.is("Krone") ? "Steine:" : "Farbe:"));
				out.add(Item.tile("Nichts", "", !head.isEnabled(), () -> head.setEnabled(false)));
				for (String name : CosmeticRegistry.HEADS.values()) {
					out.add(Item.tile(name, "3D", head.isEnabled() && head.type.get().equals(name), () -> {
						head.type.set(name);
						head.setEnabled(true);
					}));
				}
			}
			case 2 -> {
				WingsModule wings = WingsModule.get();
				out.add(Item.colors(wings.color, "Farbe:"));
				out.add(Item.tile("Keine", "", !wings.isEnabled(), () -> wings.setEnabled(false)));
				for (String name : CosmeticRegistry.WINGS.values()) {
					out.add(Item.tile(name, "animiert", wings.isEnabled() && wings.type.get().equals(name), () -> {
						wings.type.set(name);
						wings.setEnabled(true);
					}));
				}
			}
			case 3 -> {
				MinimeeModule mini = MinimeeModule.get();
				out.add(Item.tile("Kein Minimee", "", !mini.isEnabled(), () -> mini.setEnabled(false)));
				for (String name : CosmeticRegistry.MINIMEE_MODES.values()) {
					out.add(Item.tile(name, "Mini-Spieler", mini.isEnabled() && mini.mode.get().equals(name), () -> {
						mini.mode.set(name);
						mini.setEnabled(true);
					}));
				}
				out.add(Item.header("Wer soll es sein?", null));
				out.add(Item.setting(mini.name, "Spielername"));
				out.add(Item.hint("Leer = du selbst. Jeder Minecraft-Name geht."));
			}
			case 4 -> {
				CompanionModule buddy = (CompanionModule) NovaClient.modules().byName("Begleiter");
				out.add(Item.tile("Kein Begleiter", "", !buddy.isEnabled(), () -> buddy.setEnabled(false)));
				for (String name : buddy.type.getModes()) {
					out.add(Item.tile(name, "", buddy.isEnabled() && buddy.type.get().equals(name), () -> {
						buddy.type.set(name);
						buddy.setEnabled(true);
					}));
				}
				out.add(Item.header("Optionen", null));
				out.add(Item.setting(buddy.side, buddy.side.getName()));
				out.add(Item.hint("Begleiter sieht nur du."));
			}
			case 5 -> {
				for (String name : EFFECTS) {
					Module m = NovaClient.modules().byName(name);
					if (m == null) {
						continue;
					}
					out.add(Item.header(name, m));
					for (Setting<?> s : m.getSettings()) {
						out.add(Item.setting(s, s.getName()));
					}
				}
			}
			case 7 -> {
				ExtrasModule extras = ExtrasModule.get();
				out.add(Item.colors(extras.color, "Farbe:"));
				out.add(Item.tile("Kein Extra", "", !extras.isEnabled(), () -> extras.setEnabled(false)));
				for (String name : CosmeticRegistry.EXTRAS.values()) {
					out.add(Item.tile(name, "3D", extras.isEnabled() && extras.type.get().equals(name), () -> {
						extras.type.set(name);
						extras.setEnabled(true);
					}));
				}
			}
			default -> {
				Emotes.Active active = Emotes.localActive();
				for (Map.Entry<String, Emotes.Info> e : Emotes.ALL.entrySet()) {
					out.add(Item.tile(e.getValue().name(), "abspielen", active != null && active.id().equals(e.getKey()),
						() -> Emotes.play(e.getKey())));
				}
				EmotesModule emotes = EmotesModule.get();
				out.add(Item.header("Optionen", null));
				out.add(Item.setting(emotes.switchCamera, emotes.switchCamera.getName()));
				out.add(Item.hint("Emote-Rad im Spiel: Taste " + emotes.wheelKey.display()));
			}
		}
		return out;
	}

	/** Positions of all items (tiles in a grid, other items full width), y without scrolling. */
	private List<Placed> placed() {
		List<Placed> out = new ArrayList<>();
		int y = gridTop();
		int col = 0;
		int tw = tileW();
		for (Item it : items()) {
			if (it.kind() == Kind.TILE) {
				out.add(new Placed(it, gridX() + col * (tw + GAP), y, tw, TILE_H));
				col++;
				if (col == columns()) {
					col = 0;
					y += TILE_H + GAP;
				}
				continue;
			}
			if (col != 0) {
				col = 0;
				y += TILE_H + GAP;
			}
			int h = switch (it.kind()) {
				case HEADER -> HEADER_H;
				case COLORS -> swatchRows() * (SWATCH + 5) + 6;
				case HINT -> 12;
				default -> ROW_H;
			};
			if (it.kind() == Kind.HEADER && !out.isEmpty()) {
				y += 4;
			}
			out.add(new Placed(it, gridX(), y, gridW(), h));
			y += h + 3;
		}
		return out;
	}

	private int contentBottom(List<Placed> list) {
		int b = gridTop();
		for (Placed p : list) {
			b = Math.max(b, p.y() + p.h());
		}
		return b;
	}

	private double maxScroll() {
		return Math.max(0, contentBottom(placed()) - gridBottom() + 4);
	}

	// ------------------------------------------------------------------ colour swatches

	private int swatchesPerRow() {
		return Math.max(1, (gridW() - font.width("Steine:") - 10) / (SWATCH + 5));
	}

	private int swatchRows() {
		return (CosmeticRegistry.COLORS.size() + swatchesPerRow() - 1) / swatchesPerRow();
	}

	private int[] swatchPos(Placed p, int i) {
		int per = swatchesPerRow();
		int x = p.x() + font.width("Steine:") + 10 + (i % per) * (SWATCH + 5);
		int y = p.y() + 3 + (i / per) * (SWATCH + 5);
		return new int[] {x, y};
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		layout();
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
		int accent = ThemeModule.accent();
		Render.rounded(g, left, top, left + winW, top + winH, Render.alpha(0xFF0E0E14, ThemeModule.menuAlpha()));
		Render.roundedOutline(g, left, top, left + winW, top + winH, Render.alpha(accent, 0x70));

		renderPreview(g, accent);

		g.text(font, "COSMETICS", gridX(), top + 4, 0xFF6C6C7E, false);
		int[][] tabs = tabBounds();
		for (int i = 0; i < TABS.length; i++) {
			boolean sel = i == tab;
			int ty = tabs[i][2];
			boolean hover = Render.inside(mouseX, mouseY, tabs[i][0], ty, tabs[i][1], ty + 14);
			if (sel) {
				Render.rounded(g, tabs[i][0], ty, tabs[i][1], ty + 14, Render.alpha(accent, 0x55));
			} else if (hover) {
				Render.rounded(g, tabs[i][0], ty, tabs[i][1], ty + 14, 0x22FFFFFF);
			}
			g.text(font, TABS[i], tabs[i][0] + 5, ty + 3, sel ? 0xFFFFFFFF : 0xFF9A9AAE, false);
		}

		List<Placed> list = placed();
		g.enableScissor(gridX() - 2, gridTop() - 2, gridX() + gridW() + 2, gridBottom() + 2);
		for (Placed p : list) {
			int y = p.y() - (int) scroll;
			if (y + p.h() < gridTop() - 2 || y > gridBottom() + 2) {
				continue;
			}
			boolean hover = mouseY >= gridTop() && mouseY < gridBottom() && Render.inside(mouseX, mouseY, p.x(), y, p.x() + p.w(), y + p.h());
			renderItem(g, p, y, hover, mouseX, mouseY, accent);
		}
		g.disableScissor();
		if (maxScroll() > 0) {
			int trackH = gridBottom() - gridTop();
			int barH = Math.max(14, (int) (trackH * trackH / (double) (trackH + maxScroll())));
			int barY = gridTop() + (int) ((trackH - barH) * (scroll / maxScroll()));
			g.fill(left + winW - 7, barY, left + winW - 5, barY + barH, Render.alpha(accent, 0x90));
		}

		g.text(font, networkStatus(), gridX(), top + winH - 15, 0xFF6C6C7E, false);
	}

	private void renderItem(GuiGraphicsExtractor g, Placed p, int y, boolean hover, int mouseX, int mouseY, int accent) {
		Item it = p.item();
		int x = p.x();
		int w = p.w();
		switch (it.kind()) {
			case TILE -> {
				int bg = hover ? 0xFF22222D : 0xFF181820;
				if (it.selected()) {
					bg = Render.mix(bg, accent, 0.2F);
				}
				Render.rounded(g, x, y, x + w, y + TILE_H, bg);
				if (it.selected()) {
					Render.roundedOutline(g, x, y, x + w, y + TILE_H, Render.alpha(accent, 0xC0));
				}
				int textX = x + 7;
				if (it.capeId() != null) {
					// cape front (10x16 at 1,1 of the cape texture), animated capes show their current frame
					g.blit(RenderPipelines.GUI_TEXTURED, CapeModule.texturePath(it.capeId()), x + 5, y + 4, 1, 1, 16, 26, 10, 16, 64, 32);
					textX = x + 26;
				}
				g.text(font, font.plainSubstrByWidth(it.label(), x + w - 6 - textX), textX, y + 8, 0xFFFFFFFF, false);
				if (!it.sub().isEmpty()) {
					g.text(font, it.sub(), textX, y + 19, it.selected() ? accent : 0xFF7C7C8E, false);
				}
			}
			case HEADER -> {
				g.text(font, it.label().toUpperCase(java.util.Locale.ROOT), x, y + 3, 0xFF8E8EA4, false);
				if (it.module() != null) {
					Render.toggle(g, x + w - 22, y + 1, it.module().isEnabled(), accent);
				}
				g.fill(x + font.width(it.label().toUpperCase(java.util.Locale.ROOT)) + 6, y + 7, x + w - (it.module() != null ? 28 : 0), y + 8, 0x22FFFFFF);
			}
			case HINT -> g.text(font, font.plainSubstrByWidth(it.label(), w), x, y + 2, 0xFF7C7C8E, false);
			case COLORS -> {
				ModeSetting colors = (ModeSetting) it.setting();
				g.text(font, it.label(), x, y + 6, 0xFF9A9AAE, false);
				List<String> names = colors.getModes();
				for (int i = 0; i < names.size(); i++) {
					int[] s = swatchPos(new Placed(it, x, y, w, p.h()), i);
					String n = names.get(i);
					boolean sel = colors.get().equals(n);
					boolean over = Render.inside(mouseX, mouseY, s[0], s[1], s[0] + SWATCH, s[1] + SWATCH);
					if (sel || over) {
						Render.rounded(g, s[0] - 2, s[1] - 2, s[0] + SWATCH + 2, s[1] + SWATCH + 2, sel ? 0xFFFFFFFF : 0x80FFFFFF);
					}
					if (n.equals("Regenbogen")) {
						int[] rainbow = {0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA};
						int sw = SWATCH / rainbow.length + 1;
						for (int k = 0; k < rainbow.length; k++) {
							g.fill(s[0] + k * SWATCH / rainbow.length, s[1], Math.min(s[0] + SWATCH, s[0] + k * SWATCH / rainbow.length + sw), s[1] + SWATCH, rainbow[k]);
						}
					} else {
						Render.rounded(g, s[0], s[1], s[0] + SWATCH, s[1] + SWATCH, n.equals("Normal") ? 0xFF555566 : CosmeticRegistry.tint(n, 0));
					}
					if (n.equals("Normal")) {
						g.text(font, "N", s[0] + 4, s[1] + 3, 0xFFFFFFFF, false);
					}
					if (over) {
						g.setTooltipForNextFrame(font, Component.literal(n), mouseX, mouseY);
					}
				}
			}
			default -> {
				Render.rounded(g, x, y, x + w, y + ROW_H, hover ? 0xFF20202A : 0xFF16161E);
				g.text(font, it.label(), x + 6, y + 5, 0xFFDADAE6, false);
				Setting<?> s = it.setting();
				if (s instanceof BoolSetting b) {
					Render.toggle(g, x + w - 26, y + 4, b.isOn(), accent);
				} else if (s instanceof ModeSetting m) {
					String v = "‹ " + m.get() + " ›";
					g.text(font, v, x + w - 6 - font.width(v), y + 5, accent, false);
				} else if (s instanceof NumberSetting n) {
					String v = "‹ " + n.format() + " ›";
					g.text(font, v, x + w - 6 - font.width(v), y + 5, accent, false);
				} else if (s instanceof TextSetting t) {
					int fx0 = x + w / 2;
					boolean focused = editing == t;
					Render.rounded(g, fx0, y + 2, x + w - 4, y + ROW_H - 2, focused ? 0xFF2A2A38 : 0xFF101016);
					if (focused) {
						Render.roundedOutline(g, fx0, y + 2, x + w - 4, y + ROW_H - 2, Render.alpha(accent, 0xC0));
					}
					String v = t.get();
					boolean blink = focused && (System.currentTimeMillis() / 500) % 2 == 0;
					String shown = v.isEmpty() && !focused ? "(du selbst)" : v + (blink ? "_" : "");
					g.text(font, font.plainSubstrByWidth(shown, x + w - 10 - fx0), fx0 + 4, y + 5, v.isEmpty() && !focused ? 0xFF6C6C7E : 0xFFFFFFFF, false);
				}
			}
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

	private void renderPreview(GuiGraphicsExtractor g, int accent) {
		int x0 = left + 8;
		int y0 = top + 8;
		int x1 = left + 8 + previewW();
		int y1 = top + winH - 8;
		g.fillGradient(x0, y0, x1, y1, 0xFF1A1830, 0xFF0F0F16);
		Render.roundedOutline(g, x0, y0, x1, y1, 0x30FFFFFF);
		// time based, so the speed does not depend on the frame rate
		long now = System.nanoTime();
		float dt = Math.min(0.1F, (now - lastFrame) / 1_000_000_000F);
		lastFrame = now;
		if (!draggingPreview && System.currentTimeMillis() - lastInteraction > 2500) {
			yaw += SPIN_SPEED * dt;
		}
		LocalPlayer p = minecraft.player;
		if (p == null) {
			return;
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

	private void changed() {
		NovaNetwork.requestSync();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		layout();
		double mx = event.x();
		double my = event.y();
		lastInteraction = System.currentTimeMillis();
		int button = event.button();
		if (button != LEFT && button != RIGHT) {
			return super.mouseClicked(event, doubleClick);
		}
		editing = null;
		if (button == LEFT && Render.inside(mx, my, left + 8, top + 8, left + 8 + previewW(), top + winH - 8)) {
			draggingPreview = true;
			return true;
		}
		int[][] tabs = tabBounds();
		for (int i = 0; i < tabs.length; i++) {
			if (Render.inside(mx, my, tabs[i][0], tabs[i][2], tabs[i][1], tabs[i][2] + 14)) {
				tab = i;
				lastTab = i;
				scroll = 0;
				return true;
			}
		}
		if (my < gridTop() || my >= gridBottom()) {
			return super.mouseClicked(event, doubleClick);
		}
		int dir = button == RIGHT ? -1 : 1;
		for (Placed p : placed()) {
			int y = p.y() - (int) scroll;
			if (!Render.inside(mx, my, p.x(), y, p.x() + p.w(), y + p.h())) {
				continue;
			}
			Item it = p.item();
			switch (it.kind()) {
				case TILE -> it.action().run();
				case HEADER -> {
					if (it.module() != null && mx >= p.x() + p.w() - 30) {
						it.module().toggle();
					}
				}
				case COLORS -> {
					ModeSetting colors = (ModeSetting) it.setting();
					for (int i = 0; i < colors.getModes().size(); i++) {
						int[] s = swatchPos(new Placed(it, p.x(), y, p.w(), p.h()), i);
						if (Render.inside(mx, my, s[0], s[1], s[0] + SWATCH, s[1] + SWATCH)) {
							colors.set(colors.getModes().get(i));
						}
					}
				}
				case BOOL -> ((BoolSetting) it.setting()).toggle();
				case MODE -> ((ModeSetting) it.setting()).cycle(dir);
				case NUMBER -> {
					NumberSetting n = (NumberSetting) it.setting();
					double v = n.get() + dir * n.getStep();
					n.set(v > n.getMax() + 1e-9 ? n.getMin() : v < n.getMin() - 1e-9 ? n.getMax() : v);
				}
				case TEXT -> editing = (TextSetting) it.setting();
				default -> {
				}
			}
			changed();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (editing == null || !event.isAllowedChatCharacter()) {
			return false;
		}
		String c = event.codepointAsString();
		if (editing == MinimeeModule.get().name && !c.matches("[A-Za-z0-9_]")) {
			return true;
		}
		editing.set(editing.get() + c);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (editing != null) {
			String v = editing.get();
			if (key == InputConstants.KEY_BACKSPACE && !v.isEmpty()) {
				editing.set(v.substring(0, v.offsetByCodePoints(v.length(), -1)));
			} else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_ESCAPE) {
				editing = null;
				changed();
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingPreview) {
			yaw -= (float) dx * DRAG_SPEED;
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
		return new int[] {(t[i][0] + t[i][1]) / 2, t[i][2] + 7};
	}

	private @Nullable Placed find(Kind kind, String label) {
		for (Placed p : placed()) {
			if (p.item().kind() == kind && p.item().label().equals(label)) {
				return p;
			}
		}
		return null;
	}

	public int @Nullable [] tileCenter(String label) {
		layout();
		Placed p = find(Kind.TILE, label);
		return p == null ? null : new int[] {p.x() + p.w() / 2, p.y() - (int) scroll + p.h() / 2};
	}

	/** Centre of a setting row (by its label) on the current tab. */
	public int @Nullable [] rowCenter(String label) {
		layout();
		for (Placed p : placed()) {
			if (p.item().setting() != null && p.item().label().equals(label)) {
				return new int[] {p.x() + p.w() - 30, p.y() - (int) scroll + p.h() / 2};
			}
		}
		return null;
	}

	/** Centre of the on/off switch of a module header on the effects tab. */
	public int @Nullable [] headerToggle(String label) {
		layout();
		Placed p = find(Kind.HEADER, label);
		return p == null ? null : new int[] {p.x() + p.w() - 12, p.y() - (int) scroll + 6};
	}

	public int[] swatchCenter(String color) {
		layout();
		for (Placed p : placed()) {
			if (p.item().kind() == Kind.COLORS) {
				int i = ((ModeSetting) p.item().setting()).getModes().indexOf(color);
				int[] s = swatchPos(new Placed(p.item(), p.x(), p.y() - (int) scroll, p.w(), p.h()), Math.max(0, i));
				return new int[] {s[0] + SWATCH / 2, s[1] + SWATCH / 2};
			}
		}
		return new int[] {0, 0};
	}

	/** Scrolls so the item with this label is visible (used by tests). */
	public void scrollTo(String label) {
		layout();
		for (Placed p : placed()) {
			if (p.item().label().equals(label)) {
				scroll = Math.max(0, Math.min(maxScroll(), p.y() - gridTop() - 4));
				return;
			}
		}
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
		this.lastInteraction = System.currentTimeMillis() + 60_000L;
	}

	public float getYaw() {
		return yaw;
	}
}
