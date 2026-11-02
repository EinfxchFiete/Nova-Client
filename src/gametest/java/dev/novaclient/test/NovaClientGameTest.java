package dev.novaclient.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.gui.ClickGuiScreen;
import dev.novaclient.gui.HudEditorScreen;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.module.visual.ZoomModule;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;

/** Drives the real game: opens a world, uses every feature and takes screenshots. */
public class NovaClientGameTest implements FabricClientGameTest {
	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static Module module(String name) {
		Module m = NovaClient.modules().byName(name);
		check(m != null, "module missing: " + name);
		return m;
	}

	private static void enable(ClientGameTestContext ctx, String... names) {
		ctx.runOnClient(mc -> {
			for (String n : names) {
				module(n).setEnabled(true);
			}
		});
	}

	/** Moves the real cursor to a GUI-scaled position and clicks. */
	private static void click(ClientGameTestContext ctx, double guiX, double guiY, int button) {
		int scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
		ctx.getInput().setCursorPos(guiX * scale, guiY * scale);
		ctx.waitTick();
		ctx.getInput().pressMouse(button);
		ctx.waitTick();
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		TestInput input = ctx.getInput();
		try (TestSingleplayerContext sp = ctx.worldBuilder()
			.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
			.create()) {
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("gamerule advance_time false");
			sp.getServer().runCommand("weather clear");
			ctx.waitTicks(40);
			ctx.takeScreenshot("01_hud_default");

			// ---------------------------------------------------------------- HUD with all elements
			enable(ctx, "Uhrzeit", "Server-IP", "Geschwindigkeit", "Arbeitsspeicher", "Tag-Zähler");
			sp.getServer().runCommand("effect give @a minecraft:speed 120 1");
			sp.getServer().runCommand("effect give @a minecraft:strength 60 0");
			sp.getServer().runCommand("item replace entity @a armor.head with minecraft:diamond_helmet");
			sp.getServer().runCommand("item replace entity @a armor.chest with minecraft:iron_chestplate");
			sp.getServer().runCommand("item replace entity @a armor.legs with minecraft:golden_leggings");
			sp.getServer().runCommand("item replace entity @a armor.feet with minecraft:leather_boots");
			sp.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword");
			ctx.waitTicks(10);
			input.pressMouse(0);
			input.pressMouse(0);
			input.pressMouse(1);
			ctx.waitTicks(2);
			int cps = ctx.computeOnClient(mc -> dev.novaclient.util.CpsTracker.left());
			check(cps == 2, "CPS should count 2 left clicks but was " + cps);
			ctx.takeScreenshot("02_hud_all");

			// ---------------------------------------------------------------- ClickGUI via real key press
			input.pressKey(InputConstants.KEY_RSHIFT);
			ctx.waitForScreen(ClickGuiScreen.class);
			ctx.waitTicks(2);
			ctx.takeScreenshot("03_clickgui_hud");

			// click the "Visuals" category (second entry of the sidebar)
			int[] layout = ctx.computeOnClient(mc -> {
				int w = mc.getWindow().getGuiScaledWidth();
				int h = mc.getWindow().getGuiScaledHeight();
				int winW = Math.min(420, w - 16);
				int winH = Math.min(270, h - 16);
				return new int[] {(w - winW) / 2, (h - winH) / 2, winW, winH};
			});
			int left = layout[0];
			int top = layout[1];
			int winW = layout[2];
			click(ctx, left + 40, top + 32 + 22 + 9, 0);
			ctx.takeScreenshot("04_clickgui_visuals");

			// rows in Visuals: Zoom, Fullbright, No Hurt Cam, Low Fire (24px + 2px gap each)
			int cx = left + 96 + 8;
			int cw = winW - 96 - 16;
			int rowFullbright = top + 30 + 26 + 12;
			boolean before = ctx.computeOnClient(mc -> module("Fullbright").isEnabled());
			click(ctx, cx + 40, rowFullbright, 0);
			boolean after = ctx.computeOnClient(mc -> module("Fullbright").isEnabled());
			check(before != after, "left click on Fullbright row did not toggle it");
			check(after, "Fullbright should now be enabled");

			// right click Zoom -> settings expand
			click(ctx, cx + 40, top + 30 + 12, 1);
			ctx.waitTicks(2);
			ctx.takeScreenshot("05_clickgui_zoom_settings");

			// drag the first slider (Zoom-Stärke) to the far right
			int sliderRowY = top + 30 + 26 + 9;
			int sliderX = cx + cw - 8 - 90;
			click(ctx, sliderX + 89.9, sliderRowY, 0);
			double zoomFactor = ctx.computeOnClient(mc -> ((ZoomModule) module("Zoom")).zoomFactor.get());
			check(zoomFactor > 45, "slider click should set zoom factor near max but was " + zoomFactor);
			ctx.runOnClient(mc -> ((ZoomModule) module("Zoom")).zoomFactor.set(4.0));

			// keybind listening: click the "Tastenbelegung" row and press K
			int settingsCount = ctx.computeOnClient(mc -> module("Zoom").getSettings().size());
			int keyRowY = top + 30 + 26 + settingsCount * 20 + 9;
			click(ctx, cx + 60, keyRowY, 0);
			input.pressKey(InputConstants.KEY_K);
			ctx.waitTick();
			int bound = ctx.computeOnClient(mc -> module("Zoom").getKeybind());
			check(bound == InputConstants.KEY_K, "keybind should be K but was " + bound);
			ctx.takeScreenshot("06_clickgui_keybind");
			ctx.runOnClient(mc -> module("Zoom").setKeybind(0));

			// scrolling must not crash
			input.scroll(-3);
			ctx.waitTick();
			input.scroll(3);
			ctx.waitTick();

			// Utility category
			click(ctx, left + 40, top + 32 + 44 + 9, 0);
			ctx.takeScreenshot("07_clickgui_utility");

			// ESC closes and saves config
			input.pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitFor(mc -> mc.gui.screen() == null);
			Path cfg = FabricLoader.getInstance().getConfigDir().resolve("novaclient.json");
			check(Files.isRegularFile(cfg), "config file was not written");

			// ---------------------------------------------------------------- HUD editor + dragging
			ctx.setScreen(() -> new HudEditorScreen(null));
			ctx.waitTicks(2);
			ctx.takeScreenshot("08_hud_editor");
			int[] fps = ctx.computeOnClient(mc -> {
				HudModule m = (HudModule) module("FPS");
				int w = mc.getWindow().getGuiScaledWidth();
				int h = mc.getWindow().getGuiScaledHeight();
				return new int[] {m.getScreenX(w, true) + 4, m.getScreenY(h, true) + 4};
			});
			int scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
			int fpsXBefore = fps[0];
			input.setCursorPos(fps[0] * scale, fps[1] * scale);
			ctx.waitTick();
			input.holdMouse(0);
			ctx.waitTick();
			for (int i = 0; i < 10; i++) {
				input.moveCursor(10 * scale, 6 * scale);
				ctx.waitTick();
			}
			input.releaseMouse(0);
			ctx.waitTick();
			int fpsXAfter = ctx.computeOnClient(mc -> ((HudModule) module("FPS")).getScreenX(mc.getWindow().getGuiScaledWidth(), true)) + 4;
			check(fpsXAfter - fpsXBefore > 50, "dragging FPS element did not move it (" + fpsXBefore + " -> " + fpsXAfter + ")");
			ctx.takeScreenshot("09_hud_editor_dragged");
			input.pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitFor(mc -> mc.gui.screen() == null);

			// ---------------------------------------------------------------- Fullbright at midnight
			sp.getServer().runCommand("time set midnight");
			ctx.waitTicks(20);
			ctx.takeScreenshot("10_fullbright_midnight");
			ctx.runOnClient(mc -> module("Fullbright").setEnabled(false));
			ctx.waitTicks(20);
			ctx.takeScreenshot("11_no_fullbright_midnight");
			sp.getServer().runCommand("time set noon");

			// ---------------------------------------------------------------- Zoom (default key C) + scroll
			sp.getServer().runCommand("tp @a 0 -60 0 180 0");
			sp.getServer().runCommand("fill -3 -60 -12 3 -54 -12 minecraft:gold_block");
			sp.getServer().runCommand("fill -1 -59 -11 1 -57 -11 minecraft:diamond_block");
			ctx.waitTicks(20);
			ctx.takeScreenshot("12a_before_zoom");
			float normalFov = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
			input.holdKey(InputConstants.KEY_C);
			ctx.waitTicks(15);
			check(ctx.computeOnClient(mc -> ZoomModule.isZooming()), "zoom key did not start zooming");
			float zoomFov = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
			NovaClient.LOGGER.info("TEST fov normal={} zoomed={}", normalFov, zoomFov);
			check(zoomFov < normalFov / 3.0F, "camera fov did not shrink while zooming: " + normalFov + " -> " + zoomFov);
			int slotBefore = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
			input.scroll(2);
			ctx.waitTicks(5);
			int slotAfter = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
			check(slotBefore == slotAfter, "scrolling while zooming must not change hotbar slot");
			ctx.takeScreenshot("12_zoom");
			input.releaseKey(InputConstants.KEY_C);
			ctx.waitTicks(20);
			check(!ctx.computeOnClient(mc -> ZoomModule.isZooming()), "zoom did not stop");
			float afterFov = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
			check(Math.abs(afterFov - normalFov) < 1.0F, "fov not restored after zoom: " + afterFov);

			// ---------------------------------------------------------------- Low fire + no hurt cam
			sp.getServer().runCommand("effect give @a minecraft:fire_resistance 60 0");
			sp.getServer().runOnServer(server -> {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					p.setRemainingFireTicks(400);
				}
			});
			ctx.waitTicks(10);
			check(ctx.computeOnClient(mc -> mc.player.isOnFire()), "player should be burning for the fire test");
			ctx.takeScreenshot("13a_fire_vanilla");
			sp.getServer().runCommand("effect clear @a minecraft:fire_resistance");
			sp.getServer().runCommand("effect give @a minecraft:regeneration 60 4");
			sp.getServer().runCommand("effect give @a minecraft:resistance 60 4");
			ctx.waitTicks(3);
			ctx.takeScreenshot("13b_fire_vanilla_no_resistance");
			enable(ctx, "Low Fire", "No Hurt Cam");
			for (double off : new double[] {0.1, 0.2, 0.3}) {
				ctx.runOnClient(mc -> ((dev.novaclient.module.visual.LowFireModule) module("Low Fire")).offset.set(off));
				ctx.waitTicks(2);
				ctx.takeScreenshot("13c_fire_low_" + (int) (off * 100));
			}
			check(ctx.computeOnClient(mc -> mc.player.isOnFire()), "player should still burn");
			sp.getServer().runCommand("damage @p 2 minecraft:generic");
			ctx.waitTicks(3);
			ctx.takeScreenshot("14_no_hurt_cam");
			sp.getServer().runCommand("effect clear @a minecraft:fire_resistance");
			sp.getServer().runOnServer(server -> {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					p.setRemainingFireTicks(0);
				}
			});

			// ---------------------------------------------------------------- Toggle sprint / sneak
			enable(ctx, "Toggle Sprint", "Toggle Sneak");
			input.holdKey(o -> o.keyUp);
			ctx.waitTicks(20);
			boolean sprinting = ctx.computeOnClient(mc -> mc.player.isSprinting());
			input.releaseKey(o -> o.keyUp);
			check(sprinting, "Toggle Sprint: player should sprint while walking forward");
			check(ctx.computeOnClient(mc -> mc.options.toggleCrouch().get()), "Toggle Sneak should enable toggle crouch");
			ctx.runOnClient(mc -> {
				module("Toggle Sprint").setEnabled(false);
				module("Toggle Sneak").setEnabled(false);
			});
			ctx.waitTicks(2);
			check(!ctx.computeOnClient(mc -> mc.options.toggleCrouch().get()), "Toggle Sneak should restore vanilla option");

			// ---------------------------------------------------------------- module keybind toggles a module
			ctx.runOnClient(mc -> module("FPS").setKeybind(InputConstants.KEY_J));
			boolean fpsBefore = ctx.computeOnClient(mc -> module("FPS").isEnabled());
			input.pressKey(InputConstants.KEY_J);
			ctx.waitTicks(2);
			check(fpsBefore != ctx.computeOnClient(mc -> module("FPS").isEnabled()), "module keybind did not toggle FPS");
			ctx.runOnClient(mc -> {
				module("FPS").setEnabled(true);
				module("FPS").setKeybind(0);
			});

			// ---------------------------------------------------------------- theme rainbow + F1 hide
			ctx.runOnClient(mc -> ((dev.novaclient.module.hud.ThemeModule) module("Theme")).rainbow.set(true));
			ctx.waitTicks(5);
			ctx.takeScreenshot("15_rainbow_theme");
			ctx.runOnClient(mc -> ((dev.novaclient.module.hud.ThemeModule) module("Theme")).rainbow.set(false));

			// ---------------------------------------------------------------- config round trip
			ctx.runOnClient(mc -> {
				NovaClient.config().save();
				HudModule f = (HudModule) module("FPS");
				String ax = f.getAnchorX().name();
				int ox = f.getOffsetX();
				f.setPosition("START", 0, "START", 0);
				NovaClient.config().load();
				check(f.getAnchorX().name().equals(ax) && f.getOffsetX() == ox, "config load did not restore FPS position");
			});
		}
		NovaClient.LOGGER.info("NOVA GAMETEST PASSED");
	}
}
