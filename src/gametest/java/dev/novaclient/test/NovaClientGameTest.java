package dev.novaclient.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.NovaClient;
import dev.novaclient.gui.ClickGuiScreen;
import dev.novaclient.gui.HudEditorScreen;
import dev.novaclient.module.Module;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.module.visual.CrosshairModule;
import dev.novaclient.module.visual.FreelookModule;
import dev.novaclient.module.visual.LowFireModule;
import dev.novaclient.module.visual.ZoomModule;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.util.CombatTracker;
import dev.novaclient.util.CpsTracker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerPlayer;

/**
 * Drives the real game: opens a world, uses every feature with real key presses and mouse clicks
 * and takes screenshots. Mouse buttons use the Minecraft 26.x (SDL) numbering: left = 1, right = 3.
 */
public class NovaClientGameTest implements FabricClientGameTest {
	private static final int LMB = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int RMB = InputConstants.MOUSE_BUTTON_RIGHT;

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

	private static boolean enabled(ClientGameTestContext ctx, String name) {
		return ctx.computeOnClient(mc -> module(name).isEnabled());
	}

	/** Moves the real cursor to a GUI-scaled position and clicks with a real mouse button. */
	private static void click(ClientGameTestContext ctx, int[] guiPos, int button) {
		check(guiPos != null, "click target not visible");
		int scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
		ctx.getInput().setCursorPos(guiPos[0] * scale + 0.5, guiPos[1] * scale + 0.5);
		ctx.waitTick();
		ctx.getInput().pressMouse(button);
		ctx.waitTick();
	}

	private static <T> T gui(ClientGameTestContext ctx, Function<ClickGuiScreen, T> f) {
		return ctx.computeOnClient(mc -> {
			check(mc.gui.screen() instanceof ClickGuiScreen, "ClickGUI is not open but " + mc.gui.screen());
			return f.apply((ClickGuiScreen) mc.gui.screen());
		});
	}

	/** Reads TextInputManager.textInputEnabled: without it SDL sends no typed characters. */
	private static boolean textInputEnabled(Minecraft mc) {
		try {
			java.lang.reflect.Field f = mc.textInputManager().getClass().getDeclaredField("textInputEnabled");
			f.setAccessible(true);
			return f.getBoolean(mc.textInputManager());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final int BACKEND_PORT = 18765;

	/** Starts the Nova server (backend/server.js) locally with the test login enabled. */
	private static Process startBackend() {
		try {
			java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();
			java.nio.file.Path script = null;
			for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParent()) {
				if (java.nio.file.Files.isRegularFile(dir.resolve("backend/server.js"))) {
					script = dir.resolve("backend/server.js");
					break;
				}
			}
			check(script != null, "backend/server.js not found");
			ProcessBuilder pb = new ProcessBuilder("node", script.toString());
			pb.environment().put("PORT", String.valueOf(BACKEND_PORT));
			pb.environment().put("NOVA_DEV", "1");
			pb.redirectErrorStream(true);
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			Process p = pb.start();
			java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
			for (int i = 0; i < 50; i++) {
				try {
					var res = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + BACKEND_PORT + "/health")).build(),
						java.net.http.HttpResponse.BodyHandlers.ofString());
					if (res.statusCode() == 200) {
						return p;
					}
				} catch (java.io.IOException e) {
					Thread.sleep(100);
				}
			}
			throw new AssertionError("backend did not start");
		} catch (InterruptedException | java.io.IOException e) {
			throw new AssertionError(e);
		}
	}

	private static String postJson(String url, String body, String token) {
		try {
			var b = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).header("Content-Type", "application/json")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString(body));
			if (token != null) {
				b.header("Authorization", "Bearer " + token);
			}
			var res = java.net.http.HttpClient.newHttpClient().send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
			check(res.statusCode() == 200, "HTTP " + res.statusCode() + " from " + url + ": " + res.body());
			return res.body();
		} catch (InterruptedException | java.io.IOException e) {
			throw new AssertionError(e);
		}
	}

	private static String devToken(String server, java.util.UUID uuid, String name) {
		String body = postJson(server + "/v1/auth/dev", "{\"uuid\":\"" + uuid + "\",\"name\":\"" + name + "\"}", null);
		return com.google.gson.JsonParser.parseString(body).getAsJsonObject().get("token").getAsString();
	}

	/** Publishes the status of the fake second Nova player. */
	/** True if a chat message containing this text was shown. */
	private static boolean chatContains(net.minecraft.client.Minecraft mc, String text) {
		try {
			Object state = mc.gui.hud.getChat().storeState();
			java.lang.reflect.Field f = state.getClass().getDeclaredField("messages");
			f.setAccessible(true);
			for (Object m : (java.util.List<?>) f.get(state)) {
				if (((net.minecraft.client.multiplayer.chat.GuiMessage) m).content().getString().contains(text)) {
					return true;
				}
			}
			return false;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static String readConfig(String file) {
		try {
			return java.nio.file.Files.readString(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("novaclient").resolve(file));
		} catch (java.io.IOException e) {
			return "";
		}
	}

	private static int chatSize(net.minecraft.client.Minecraft mc) {
		try {
			Object state = mc.gui.hud.getChat().storeState();
			java.lang.reflect.Field f = state.getClass().getDeclaredField("messages");
			f.setAccessible(true);
			return ((java.util.List<?>) f.get(state)).size();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static java.awt.image.BufferedImage testCover(int rgb) {
		var img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 64; y++) {
			for (int x = 0; x < 64; x++) {
				img.setRGB(x, y, (x + y) % 16 < 8 ? rgb : 0xFFFFFF);
			}
		}
		return img;
	}

	/** Flipp1009 adds us as a friend and shares his server. */
	private static void pushFriendRequest(String server, String token, java.util.UUID me) {
		postJson(server + "/v1/sync", "{\"status\":{\"cosmetics\":{\"cape\":\"anim:fire\",\"head\":\"tophat\"},\"server\":\"play.nova-test.net\"},"
			+ "\"players\":[],\"friends\":[\"" + me + "\"]}", token);
	}

	private static void pushRemote(String server, String token, String emote) {
		postJson(server + "/v1/sync", "{\"status\":{\"cosmetics\":{\"cape\":\"anim:fire\",\"head\":\"tophat\",\"headColor\":\"Rot\","
			+ "\"wings\":\"dragon\",\"wingsColor\":\"\",\"minimee\":{\"name\":\"EinfxchFiete\",\"mode\":\"pocket\"},\"extra\":\"foxtail\",\"extraColor\":\"\"},"
			+ "\"song\":{\"title\":\"Blinding Lights\",\"artist\":\"The Weeknd\",\"pos\":83000,\"len\":200000,\"playing\":true},"
			+ "\"emote\":{\"id\":\"" + emote + "\",\"age\":0}},\"players\":[]}", token);
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		TestInput input = ctx.getInput();
		// default server + migration of old empty addresses; keep the real server out of the test run
		ctx.runOnClient(mc -> {
			var net = (dev.novaclient.module.utility.NetworkModule) module("Nova-Netzwerk");
			check(net.serverUrl().equals(dev.novaclient.module.utility.NetworkModule.DEFAULT_SERVER)
				&& net.serverUrl().startsWith("https://"), "unexpected default server: " + net.serverUrl());
			net.server.fromJson(new com.google.gson.JsonPrimitive(""));
			check(net.serverUrl().equals(dev.novaclient.module.utility.NetworkModule.DEFAULT_SERVER),
				"old empty address was not migrated");
			net.loadEnabled(false);
		});
		try (TestSingleplayerContext sp = ctx.worldBuilder()
			.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
			.create()) {
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("gamerule advance_time false");
			sp.getServer().runCommand("gamerule advance_weather false");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("gamerule show_advancement_messages false");
			sp.getServer().runCommand("tp @a 0 -60 0 180 0");
			ctx.waitTicks(40);
			ctx.takeScreenshot("01_hud_default");

			// ---------------------------------------------------------------- CPS with real button numbers
			input.pressMouse(LMB);
			input.pressMouse(LMB);
			input.pressMouse(RMB);
			ctx.waitTicks(2);
			int left = ctx.computeOnClient(mc -> CpsTracker.left());
			int right = ctx.computeOnClient(mc -> CpsTracker.right());
			check(left == 2, "left CPS should be 2 but was " + left);
			check(right == 1, "right CPS should be 1 but was " + right);
			ctx.takeScreenshot("02_keystrokes_cps");

			// ---------------------------------------------------------------- open menu with the real key
			input.pressKey(InputConstants.KEY_RSHIFT);
			ctx.waitForScreen(dev.novaclient.gui.NovaHubScreen.class);
			ctx.takeScreenshot("02b_hub");
			click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.NovaHubScreen) mc.gui.screen()).cardCenter(0)), LMB);
			ctx.waitForScreen(ClickGuiScreen.class);
			ctx.waitTicks(2);
			check(ctx.computeOnClient(mc -> textInputEnabled(mc)), "SDL text input must be on while the menu is open (search typing)");
			ctx.takeScreenshot("03_menu_all");

			// Visuals tab, then clicking a card toggles the module
			click(ctx, gui(ctx, s -> s.tabCenter(2)), LMB);
			Module fullbright = ctx.computeOnClient(mc -> module("Fullbright"));
			check(!enabled(ctx, "Fullbright"), "Fullbright should start disabled");
			click(ctx, gui(ctx, s -> s.cardCenter(fullbright)), LMB);
			check(enabled(ctx, "Fullbright"), "left click on the Fullbright card did not enable it");
			click(ctx, gui(ctx, s -> s.cardCenter(fullbright)), LMB);
			check(!enabled(ctx, "Fullbright"), "second click did not disable Fullbright");
			click(ctx, gui(ctx, s -> s.cardCenter(fullbright)), LMB);

			ctx.takeScreenshot("04_menu_visuals");

			// right click Custom Crosshair -> settings page
			Module crosshair = ctx.computeOnClient(mc -> module("Custom Crosshair"));
			click(ctx, gui(ctx, s -> s.cardCenter(crosshair)), RMB);
			check(gui(ctx, s -> s.getSettingsModule()) == crosshair, "right click did not open the crosshair settings");
			check(!enabled(ctx, "Custom Crosshair"), "right click must not toggle the module");
			// row 0 = Stil (mode): click cycles
			click(ctx, gui(ctx, s -> s.rowPoint(0, -1)), LMB);
			String style = ctx.computeOnClient(mc -> ((CrosshairModule) module("Custom Crosshair")).style.get());
			check(style.equals("Kreuz + Punkt"), "mode click should cycle to 'Kreuz + Punkt' but is " + style);
			// row 2 = Größe (slider): click at the far right
			click(ctx, gui(ctx, s -> s.rowPoint(2, 1.0)), LMB);
			int size = ctx.computeOnClient(mc -> ((CrosshairModule) module("Custom Crosshair")).size.getInt());
			check(size == 12, "slider click should set size 12 but was " + size);
			click(ctx, gui(ctx, s -> s.rowPoint(2, 0.4)), LMB);
			// row 6 = Umrandung (bool) - scroll down first, like a player would on a small window
			input.setCursorPos(10, 10);
			ctx.waitTick();
			input.scroll(-10);
			ctx.waitTick();
			boolean outlineBefore = ctx.computeOnClient(mc -> ((CrosshairModule) module("Custom Crosshair")).outline.isOn());
			click(ctx, gui(ctx, s -> s.rowPoint(6, -1)), LMB);
			check(outlineBefore != ctx.computeOnClient(mc -> ((CrosshairModule) module("Custom Crosshair")).outline.isOn()),
				"bool setting click did not toggle");
			click(ctx, gui(ctx, s -> s.rowPoint(6, -1)), LMB);
			// keybind row: click, press K
			int keyRow = ctx.computeOnClient(mc -> module("Custom Crosshair").getSettings().size());
			click(ctx, gui(ctx, s -> s.rowPoint(keyRow, -1)), LMB);
			ctx.takeScreenshot("05_settings_listening");
			input.pressKey(InputConstants.KEY_K);
			ctx.waitTick();
			check(ctx.computeOnClient(mc -> module("Custom Crosshair").getKeybind()) == InputConstants.KEY_K, "keybind K not set");
			ctx.takeScreenshot("06_settings_crosshair");
			// ESC while listening removes the keybind
			click(ctx, gui(ctx, s -> s.rowPoint(keyRow, -1)), LMB);
			input.pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTick();
			check(ctx.computeOnClient(mc -> module("Custom Crosshair").getKeybind()) == 0, "ESC did not clear the keybind");
			check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof ClickGuiScreen), "ESC while binding must not close the menu");
			// back button
			click(ctx, gui(ctx, s -> s.backButtonCenter()), LMB);
			check(gui(ctx, s -> s.getSettingsModule()) == null, "back button did not return to the grid");

			// menu icon on the Zoom card opens settings
			Module zoom = ctx.computeOnClient(mc -> module("Zoom"));
			click(ctx, gui(ctx, s -> s.gearCenter(zoom)), LMB);
			check(gui(ctx, s -> s.getSettingsModule()) == zoom, "menu icon did not open the Zoom settings");
			check(enabled(ctx, "Zoom"), "menu icon must not toggle Zoom");
			ctx.takeScreenshot("07_settings_zoom");
			input.pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTick();
			check(gui(ctx, s -> s.getSettingsModule()) == null, "ESC should go back from settings to the grid");

			// search
			click(ctx, gui(ctx, s -> s.tabCenter(0)), LMB);
			input.typeChars("free");
			ctx.waitTicks(2);
			check(gui(ctx, s -> s.getSearch()).equals("free"), "search text not typed");
			Module freelook = ctx.computeOnClient(mc -> module("Freelook"));
			check(gui(ctx, s -> s.cardCenter(freelook)) != null, "Freelook not found by search");
			check(gui(ctx, s -> s.cardCenter(fullbright)) == null, "search should hide Fullbright");
			ctx.takeScreenshot("08_menu_search");
			input.pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTick();
			check(gui(ctx, s -> s.getSearch()).isEmpty(), "ESC should clear the search");

			// HUD editor via footer button
			click(ctx, gui(ctx, s -> s.hudEditorButtonCenter()), LMB);
			ctx.waitForScreen(HudEditorScreen.class);
			ctx.waitTicks(2);
			ctx.takeScreenshot("09_hud_editor");
			int[] listBtn = ctx.computeOnClient(mc -> ((HudEditorScreen) mc.gui.screen()).buttonCenter(0));
			click(ctx, listBtn, LMB);
			check(ctx.computeOnClient(mc -> ((HudEditorScreen) mc.gui.screen()).isListOpen()), "Elemente list did not open");
			int biomeIndex = ctx.computeOnClient(mc -> NovaClient.modules().getHudModules().indexOf((HudModule) module("Biom")));
			int[] biomeRow = ctx.computeOnClient(mc -> ((HudEditorScreen) mc.gui.screen()).listRowCenter(biomeIndex));
			ctx.waitTick();
			ctx.takeScreenshot("10_hud_editor_list");
			if (biomeRow[1] < ctx.computeOnClient(mc -> mc.getWindow().getGuiScaledHeight()) - 10) {
				click(ctx, biomeRow, LMB);
				check(enabled(ctx, "Biom"), "clicking Biom in the element list did not enable it");
			} else {
				enable(ctx, "Biom");
			}
			click(ctx, listBtn, LMB);
			// drag FPS with the real left button
			int[] fps = ctx.computeOnClient(mc -> {
				HudModule m = (HudModule) module("FPS");
				return new int[] {m.getScreenX(mc.getWindow().getGuiScaledWidth(), true) + 4, m.getScreenY(mc.getWindow().getGuiScaledHeight(), true) + 4};
			});
			int scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
			input.setCursorPos(fps[0] * scale, fps[1] * scale);
			ctx.waitTick();
			input.holdMouse(LMB);
			ctx.waitTick();
			for (int i = 0; i < 10; i++) {
				input.moveCursor(12 * scale, 8 * scale);
				ctx.waitTick();
			}
			input.releaseMouse(LMB);
			ctx.waitTick();
			int fpsAfter = ctx.computeOnClient(mc -> ((HudModule) module("FPS")).getScreenX(mc.getWindow().getGuiScaledWidth(), true)) + 4;
			check(fpsAfter - fps[0] > 60, "dragging FPS did not move it (" + fps[0] + " -> " + fpsAfter + ")");
			ctx.takeScreenshot("11_hud_editor_dragged");
			// right click element -> its settings
			int[] fpsNow = ctx.computeOnClient(mc -> {
				HudModule m = (HudModule) module("FPS");
				return new int[] {m.getScreenX(mc.getWindow().getGuiScaledWidth(), true) + 4, m.getScreenY(mc.getWindow().getGuiScaledHeight(), true) + 4};
			});
			click(ctx, fpsNow, RMB);
			check(gui(ctx, s -> s.getSettingsModule()) == module("FPS"), "right click on HUD element did not open its settings");
			ctx.setScreen(() -> null);
			ctx.waitTicks(2);
			Path cfg = FabricLoader.getInstance().getConfigDir().resolve("novaclient/profiles/Standard.json");
			check(Files.isRegularFile(cfg), "config file was not written");

			// ---------------------------------------------------------------- Fullbright
			sp.getServer().runCommand("time set midnight");
			ctx.waitTicks(20);
			ctx.takeScreenshot("12_fullbright_midnight");
			sp.getServer().runCommand("time set noon");

			// ---------------------------------------------------------------- Zoom
			sp.getServer().runCommand("fill -3 -60 -12 3 -54 -12 minecraft:gold_block");
			sp.getServer().runCommand("fill -1 -59 -11 1 -57 -11 minecraft:diamond_block");
			sp.getServer().runCommand("tp @a 0 -60 0 180 0");
			ctx.waitTicks(10);
			float normalFov = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
			input.holdKey(InputConstants.KEY_C);
			ctx.waitTicks(15);
			float zoomFov = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
			check(zoomFov < normalFov / 3.0F, "fov did not shrink while zooming: " + normalFov + " -> " + zoomFov);
			int slot = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
			input.scroll(2);
			ctx.waitTicks(3);
			check(slot == ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()), "scroll while zooming changed hotbar slot");
			ctx.takeScreenshot("13_zoom");
			input.releaseKey(InputConstants.KEY_C);
			ctx.waitTicks(20);
			check(Math.abs(ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov()) - normalFov) < 1.0F, "fov not restored");

			// ---------------------------------------------------------------- Freelook (hold Left Alt)
			check(enabled(ctx, "Freelook"), "Freelook should be enabled by default");
			float yawBefore = ctx.computeOnClient(mc -> mc.player.getYRot());
			float pitchBefore = ctx.computeOnClient(mc -> mc.player.getXRot());
			input.holdKey(InputConstants.KEY_LALT);
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> FreelookModule.isActive()), "freelook not active while holding Alt");
			check(ctx.computeOnClient(mc -> mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK), "freelook should switch to third person");
			for (int i = 0; i < 10; i++) {
				input.moveCursor(40, 10);
				ctx.waitTick();
			}
			ctx.waitTicks(3);
			float yawDuring = ctx.computeOnClient(mc -> mc.player.getYRot());
			float pitchDuring = ctx.computeOnClient(mc -> mc.player.getXRot());
			float camYaw = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().yRot());
			NovaClient.LOGGER.info("TEST freelook player yaw {} -> {}, camera yaw {}", yawBefore, yawDuring, camYaw);
			check(Math.abs(yawDuring - yawBefore) < 0.01F && Math.abs(pitchDuring - pitchBefore) < 0.01F,
				"player rotation changed during freelook");
			check(Math.abs(camYaw - yawBefore) > 5.0F, "camera did not turn during freelook (camera yaw " + camYaw + ")");
			ctx.takeScreenshot("14_freelook");
			input.releaseKey(InputConstants.KEY_LALT);
			ctx.waitTicks(5);
			check(!ctx.computeOnClient(mc -> FreelookModule.isActive()), "freelook still active after release");
			check(ctx.computeOnClient(mc -> mc.options.getCameraType() == CameraType.FIRST_PERSON), "camera not restored to first person");
			// normal mouse look works again
			input.moveCursor(40, 0);
			ctx.waitTicks(3);
			check(Math.abs(ctx.computeOnClient(mc -> mc.player.getYRot()) - yawBefore) > 1.0F, "mouse look broken after freelook");
			sp.getServer().runCommand("tp @a 0 -60 0 180 0");
			ctx.waitTicks(5);

			// ---------------------------------------------------------------- Custom crosshair styles
			enable(ctx, "Custom Crosshair");
			for (String st : new String[] {"Kreuz + Punkt", "Kreis", "T-Form"}) {
				ctx.runOnClient(mc -> {
					CrosshairModule c = (CrosshairModule) module("Custom Crosshair");
					c.style.set(st);
					c.color.set(st.equals("Kreis") ? "Theme" : "Grün");
				});
				ctx.waitTicks(2);
				ctx.takeScreenshot("15_crosshair_" + st.replace(' ', '_').replace("+", "plus"));
			}

			// ---------------------------------------------------------------- Reach + combo on an armor stand
			enable(ctx, "Reichweite", "Combo");
			sp.getServer().runCommand("summon minecraft:armor_stand 0 -60 -2.5 {NoGravity:1b}");
			sp.getServer().runCommand("tp @a 0 -60 0 180 0");
			ctx.waitTicks(5);
			input.lookAt(180.0F, 12.0F);
			ctx.waitTicks(5);
			String target = ctx.computeOnClient(mc -> mc.hitResult == null ? "null" : mc.hitResult.getType() + " " + mc.hitResult.getLocation());
			int stands = sp.getServer().computeOnServer(server -> server.overworld().getEntities(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, e -> true).size());
			NovaClient.LOGGER.info("TEST before attack: hitResult={} armorStands={}", target, stands);
			ctx.takeScreenshot("16a_before_attack");
			input.pressMouse(LMB);
			ctx.waitTicks(3);
			int combo = ctx.computeOnClient(mc -> CombatTracker.combo());
			double reach = ctx.computeOnClient(mc -> CombatTracker.lastReach());
			NovaClient.LOGGER.info("TEST combo={} reach={}", combo, reach);
			check(combo == 1, "combo should be 1 after one hit but was " + combo);
			check(reach > 1.0 && reach < 3.5, "reach out of range: " + reach);
			ctx.takeScreenshot("16_reach_combo");

			// ---------------------------------------------------------------- No weather
			sp.getServer().runCommand("weather rain");
			ctx.waitTicks(60);
			ctx.takeScreenshot("17_rain");
			enable(ctx, "Kein Regen");
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> mc.level.getRainLevel(1.0F)) == 0.0F, "client rain level should be 0");
			check(sp.getServer().computeOnServer(server -> server.overworld().getRainLevel(1.0F)) > 0.0F, "server weather must stay");
			ctx.takeScreenshot("18_no_rain");
			sp.getServer().runCommand("weather clear");
			// rain fades out over a few seconds and would extinguish the fire test below
			sp.getServer().waitFor(server -> server.overworld().getRainLevel(1.0F) == 0.0F, 400);

			// ---------------------------------------------------------------- HUD tweaks: scoreboard
			sp.getServer().runCommand("scoreboard objectives add nova dummy \"Nova Test\"");
			sp.getServer().runCommand("scoreboard objectives setdisplay sidebar nova");
			sp.getServer().runCommand("scoreboard players set Spieler nova 7");
			ctx.waitTicks(5);
			ctx.takeScreenshot("19_scoreboard");
			enable(ctx, "HUD-Tweaks");
			ctx.waitTicks(2);
			ctx.takeScreenshot("20_scoreboard_hidden");

			// ---------------------------------------------------------------- Low fire + no hurt cam
			sp.getServer().runCommand("effect give @a minecraft:fire_resistance 60 0");
			sp.getServer().runOnServer(server -> {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					p.setRemainingFireTicks(400);
				}
			});
			enable(ctx, "Low Fire", "No Hurt Cam");
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> mc.player.isOnFire()), "player should burn");
			check(ctx.computeOnClient(mc -> LowFireModule.activeOffset()) > 0, "low fire inactive");
			ctx.takeScreenshot("21_low_fire");
			sp.getServer().runOnServer(server -> {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					p.setRemainingFireTicks(0);
				}
			});

			// ---------------------------------------------------------------- toggle sprint / sneak
			enable(ctx, "Toggle Sprint", "Toggle Sneak");
			input.holdKey(o -> o.keyUp);
			ctx.waitTicks(20);
			boolean sprinting = ctx.computeOnClient(mc -> mc.player.isSprinting());
			input.releaseKey(o -> o.keyUp);
			check(sprinting, "Toggle Sprint: player should sprint");
			check(ctx.computeOnClient(mc -> mc.options.toggleCrouch().get()), "Toggle Sneak should enable toggle crouch");
			ctx.runOnClient(mc -> {
				module("Toggle Sprint").setEnabled(false);
				module("Toggle Sneak").setEnabled(false);
			});
			ctx.waitTicks(2);
			check(!ctx.computeOnClient(mc -> mc.options.toggleCrouch().get()), "Toggle Sneak should restore vanilla option");

			// ---------------------------------------------------------------- module keybind (event based)
			ctx.runOnClient(mc -> module("FPS").setKeybind(InputConstants.KEY_J));
			boolean fpsBefore = enabled(ctx, "FPS");
			input.pressKey(InputConstants.KEY_J);
			ctx.waitTicks(2);
			check(fpsBefore != enabled(ctx, "FPS"), "module keybind did not toggle FPS");
			ctx.runOnClient(mc -> {
				module("FPS").setEnabled(true);
				module("FPS").setKeybind(0);
			});

			// ---------------------------------------------------------------- all HUD modules at once
			ctx.runOnClient(mc -> {
				for (HudModule h : NovaClient.modules().getHudModules()) {
					h.setEnabled(true);
				}
				((ThemeModule) module("Theme")).rainbow.set(true);
			});
			ctx.waitTicks(5);
			ctx.takeScreenshot("22_all_hud_rainbow");
			ctx.runOnClient(mc -> ((ThemeModule) module("Theme")).rainbow.set(false));

			// ---------------------------------------------------------------- auto arrange in the HUD editor
			ctx.setScreen(() -> new HudEditorScreen(null));
			ctx.waitTicks(2);
			click(ctx, ctx.computeOnClient(mc -> ((HudEditorScreen) mc.gui.screen()).buttonCenter(1)), LMB);
			boolean overlap = ctx.computeOnClient(mc -> {
				int w = mc.getWindow().getGuiScaledWidth();
				int h = mc.getWindow().getGuiScaledHeight();
				java.util.List<HudModule> mods = NovaClient.modules().getHudModules().stream()
					.filter(m -> m.isEnabled() && m.getWidth(true) > 0 && m.getAnchorX() != HudModule.Anchor.CENTER
						&& m.getAnchorY() != HudModule.Anchor.END).toList();
				for (HudModule a : mods) {
					for (HudModule b : mods) {
						if (a != b && a.getAnchorX() == b.getAnchorX()) {
							int ay = a.getScreenY(h, true);
							int by = b.getScreenY(h, true);
							if (ay < by + b.getHeight(true) && by < ay + a.getHeight(true) && ay + a.getHeight(true) < h - 2
								&& by + b.getHeight(true) < h - 2) {
								return true;
							}
						}
					}
				}
				return false;
			});
			check(!overlap, "auto arrange left overlapping elements");
			ctx.takeScreenshot("23_auto_arranged");
			ctx.setScreen(() -> null);
			ctx.waitTicks(2);

			// ---------------------------------------------------------------- notification on keybind toggle
			ctx.runOnClient(mc -> module("Koordinaten").setKeybind(InputConstants.KEY_H));
			input.pressKey(InputConstants.KEY_H);
			ctx.waitTicks(2);
			check(ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active().stream().anyMatch(t -> t.startsWith("Koordinaten"))),
				"no notification after keybind toggle");
			ctx.takeScreenshot("24_notification");
			input.pressKey(InputConstants.KEY_H);
			ctx.runOnClient(mc -> module("Koordinaten").setKeybind(0));

			// ---------------------------------------------------------------- durability warning
			sp.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword[damage=1550]");
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active().stream().anyMatch(t -> t.contains("Haltbarkeit"))),
				"durability warning missing");
			ctx.takeScreenshot("25_durability_warning");

			// ---------------------------------------------------------------- cosmetics tab: cape via real clicks
			input.pressKey(InputConstants.KEY_RSHIFT);
			ctx.waitForScreen(dev.novaclient.gui.NovaHubScreen.class);
			input.pressKey(InputConstants.KEY_1);
			ctx.waitForScreen(ClickGuiScreen.class);
			check(gui(ctx, s -> s.visibleCardCount(dev.novaclient.module.Category.COSMETICS)) == 0,
				"cosmetics must not show up as cards in the main menu");
			click(ctx, gui(ctx, s -> s.tabCenter(4)), LMB);
			ctx.waitForScreen(dev.novaclient.gui.CosmeticsScreen.class);
			ctx.takeScreenshot("26_menu_cosmetics");
			int[] novaTile = ctx.computeOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).tileCenter("Nova"));
			click(ctx, novaTile, LMB);
			check(enabled(ctx, "Cape"), "cape tile click did not enable the cape");
			// the preview turns slowly and independent of the frame rate
			ctx.waitTicks(20 * 3);
			float yaw0 = ctx.computeOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).getYaw());
			long spinStart = System.nanoTime();
			ctx.waitTicks(20 * 3);
			float yaw1 = ctx.computeOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).getYaw());
			double spinSeconds = (System.nanoTime() - spinStart) / 1e9;
			double degPerSecond = (yaw1 - yaw0) / spinSeconds;
			check(degPerSecond > 10 && degPerSecond < 35, "preview spin speed wrong: " + degPerSecond + " degrees/s");
			input.pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitForScreen(ClickGuiScreen.class);
			String capeId = ctx.computeOnClient(mc -> {
				var c = mc.player.getSkin().cape();
				return c == null ? "none" : c.texturePath().toString();
			});
			check(capeId.equals("novaclient:textures/cape/nova.png"), "player cape texture is " + capeId);
			enable(ctx, "Nametags", "Partikel-Spur");

			// ---------------------------------------------------------------- design page: accent swatches
			click(ctx, gui(ctx, s -> s.footerButtonCenter(3)), LMB);
			check(gui(ctx, s -> s.getPage()) == ClickGuiScreen.Page.DESIGN, "design page did not open");
			click(ctx, gui(ctx, s -> s.swatchCenter(1)), LMB);
			double hue = ctx.computeOnClient(mc -> ((ThemeModule) module("Theme")).hue.get());
			check(Math.abs(hue - 220) < 0.5, "clicking the blue swatch should set hue 220 but was " + hue);
			ctx.takeScreenshot("27_design_blue");
			click(ctx, gui(ctx, s -> s.swatchCenter(0)), LMB);

			// ---------------------------------------------------------------- profiles page
			click(ctx, gui(ctx, s -> s.footerButtonCenter(2)), LMB);
			check(gui(ctx, s -> s.getPage()) == ClickGuiScreen.Page.PROFILES, "profiles page did not open");
			ctx.takeScreenshot("28_profiles");
			// preset "Minimal" is the right-most preset button
			int presetsRow = gui(ctx, s -> s.rowIndexOf("presets"));
			click(ctx, gui(ctx, s -> s.rowPointFromRight(presetsRow, 10)), LMB);
			check(ctx.computeOnClient(mc -> NovaClient.config().getActiveProfile()).equals("Minimal"), "Minimal preset not active");
			check(enabled(ctx, "FPS") && !enabled(ctx, "Koordinaten") && !enabled(ctx, "Cape"), "Minimal preset modules wrong");
			ctx.takeScreenshot("29_profile_minimal");
			// load "Standard" again with its "Laden" button
			int[] widths = ctx.computeOnClient(mc -> new int[] {mc.font.width("Löschen") + 14, mc.font.width("Laden") + 14});
			int standardRow = gui(ctx, s -> s.rowIndexOf(new ClickGuiScreen.ProfileRow("Standard")));
			click(ctx, gui(ctx, s -> s.rowPointFromRight(standardRow, widths[0] + 4 + widths[1] / 2)), LMB);
			check(ctx.computeOnClient(mc -> NovaClient.config().getActiveProfile()).equals("Standard"), "Standard not loaded");
			check(enabled(ctx, "Cape") && enabled(ctx, "Koordinaten"), "Standard profile settings not restored");
			// delete "Minimal"
			int minimalRow = gui(ctx, s -> s.rowIndexOf(new ClickGuiScreen.ProfileRow("Minimal")));
			click(ctx, gui(ctx, s -> s.rowPointFromRight(minimalRow, widths[0] / 2)), LMB);
			check(!ctx.computeOnClient(mc -> NovaClient.config().listProfiles()).contains("Minimal"), "Minimal not deleted");
			// create a named profile by typing
			int newRow = gui(ctx, s -> s.rowIndexOf("newprofile"));
			click(ctx, gui(ctx, s -> s.rowPoint(newRow, -1)), LMB);
			click(ctx, gui(ctx, s -> s.rowPointFromRight(newRow, 200)), LMB);
			input.typeChars("Mein Profil");
			input.pressKey(InputConstants.KEY_RETURN);
			ctx.waitTick();
			check(ctx.computeOnClient(mc -> NovaClient.config().getActiveProfile()).equals("Mein Profil"), "typed profile not created");
			ctx.takeScreenshot("30_profile_created");
			ctx.runOnClient(mc -> {
				NovaClient.config().switchTo("Standard");
				NovaClient.config().delete("Mein Profil");
			});

			// ---------------------------------------------------------------- rebind freelook to K in its settings
			click(ctx, gui(ctx, s -> s.tabCenter(2)), LMB);
			Module freelookMod = ctx.computeOnClient(mc -> module("Freelook"));
			click(ctx, gui(ctx, s -> s.cardCenter(freelookMod)), RMB);
			click(ctx, gui(ctx, s -> s.rowPoint(0, -1)), LMB);
			input.pressKey(InputConstants.KEY_K);
			ctx.waitTick();
			check(ctx.computeOnClient(mc -> ((FreelookModule) module("Freelook")).key.code()) == InputConstants.KEY_K, "freelook key not rebound");
			ctx.setScreen(() -> null);
			ctx.waitTicks(2);
			input.holdKey(InputConstants.KEY_K);
			ctx.waitTicks(4);
			check(ctx.computeOnClient(mc -> FreelookModule.isActive()), "freelook does not react to the new key K");
			ctx.takeScreenshot("31_freelook_indicator_cape_nametag");
			input.releaseKey(InputConstants.KEY_K);
			ctx.waitTicks(3);
			ctx.runOnClient(mc -> ((FreelookModule) module("Freelook")).key.reset());

			// ---------------------------------------------------------------- cape designs in third person front view
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			for (String design : new String[] {"Galaxie", "Feuer", "Regenbogen"}) {
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CapeModule) module("Cape")).design.set(design));
				input.holdKey(o -> o.keyUp);
				ctx.waitTicks(6);
				input.releaseKey(o -> o.keyUp);
				ctx.waitTicks(2);
				ctx.takeScreenshot("32_cape_" + design);
			}
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			// ---------------------------------------------------------------- overlays + block outline
			sp.getServer().runCommand("item replace entity @a armor.head with minecraft:carved_pumpkin");
			ctx.waitTicks(3);
			ctx.takeScreenshot("33_pumpkin_vanilla");
			enable(ctx, "Overlays", "Blockumrandung");
			input.lookAt(180.0F, 30.0F);
			ctx.waitTicks(3);
			ctx.takeScreenshot("34_pumpkin_hidden_outline");
			sp.getServer().runCommand("item replace entity @a armor.head with minecraft:diamond_helmet");

			// ---------------------------------------------------------------- chat timestamps
			enable(ctx, "Chat-Zeitstempel");
			ctx.runOnClient(mc -> mc.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("Testnachricht")));
			ctx.waitTicks(2);
			ctx.takeScreenshot("35_chat_timestamp");

			// ---------------------------------------------------------------- text input is released when the menu closes
			check(!ctx.computeOnClient(mc -> textInputEnabled(mc)), "text input should be off in game");

			// ---------------------------------------------------------------- companion, aura, halo
			enable(ctx, "Begleiter", "Partikel-Ring");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			ctx.waitTicks(15);
			check(ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_042) != null), "companion entity missing");
			ctx.takeScreenshot("37_companion_aura_halo");
			ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.set("Biene"));
			ctx.waitTicks(10);
			check(ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_042) instanceof net.minecraft.world.entity.animal.bee.Bee),
				"companion did not change to a bee");
			ctx.takeScreenshot("38_companion_bee");
			// every buddy once: it must exist, tick (animations) and walkers must stand on the ground
			java.util.List<String> buddies = ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.getModes());
			// open space and a camera from behind, so the buddies are in the picture
			sp.getServer().runCommand("tp @a 70 -60 70 0 30");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			ctx.waitTicks(10);
			StringBuilder buddyReport = new StringBuilder();
			for (int bi = 0; bi < buddies.size(); bi++) {
				String buddyName = buddies.get(bi);
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.set(buddyName));
				ctx.waitTicks(4);
				int t0 = ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_042) == null ? -1 : mc.level.getEntity(-4_242_042).tickCount);
				ctx.waitTicks(12);
				final String nameForCheck = buddyName;
				String info = ctx.computeOnClient(mc -> {
					var e = mc.level.getEntity(-4_242_042);
					if (e == null) {
						return "MISSING";
					}
					var m = (dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter");
					return e.getType().toShortString() + " ticks=" + e.tickCount + " dy=" + String.format(java.util.Locale.ROOT, "%.2f", e.getY() - mc.player.getY())
						+ " walks=" + m.walks();
				});
				buddyReport.append(nameForCheck).append(": ").append(info).append(" (t0=").append(t0).append(")\n");
				check(!info.equals("MISSING"), "buddy " + buddyName + " was not spawned");
				int t1 = ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_042).tickCount);
				check(t1 > t0, "buddy " + buddyName + " is not ticked (no animation): " + info);
				boolean walker = ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).walks());
				double dy = ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_042).getY() - mc.player.getY());
				if (walker) {
					check(Math.abs(dy) < 0.45, "walking buddy " + buddyName + " is not on the ground: dy=" + dy);
				} else {
					check(dy > 1.0, "flying buddy " + buddyName + " is not in the air: dy=" + dy);
				}
				String petModel = ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).customModel());
				if (petModel != null) {
					check(ctx.computeOnClient(mc -> dev.novaclient.cosmetics.PetModels.get(petModel) != null
						&& dev.novaclient.cosmetics.PetRenderer.lastSubmits() == 1 && mc.level.getEntity(-4_242_042).isInvisible()),
						"pet model " + petModel + " not drawn");
				}
				ctx.takeScreenshot("38b_buddy_" + bi);
			}
			NovaClient.LOGGER.info("NOVA BUDDIES\n{}", buddyReport);
			// the blobfish pet: own model on an invisible carrier
			ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.set("Blobfisch"));
			ctx.waitTicks(12);
			check(ctx.computeOnClient(mc -> {
				var b = ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).getBuddy();
				return b != null && b.isInvisible() && dev.novaclient.cosmetics.PetModels.get("blobfish") != null
					&& dev.novaclient.cosmetics.PetRenderer.lastSubmits() == 1;
			}), "blobfish pet not drawn");
			ctx.takeScreenshot("38d_pet_blobfish");
			ctx.runOnClient(mc -> mc.player.setYRot(mc.player.getYRot() + 90));
			ctx.waitTicks(15);
			ctx.takeScreenshot("38e_pet_blobfish_side");
			ctx.runOnClient(mc -> mc.player.setYRot(mc.player.getYRot() - 90));
			// CUBEES: close-ups (no HUD, first person, looking at the frozen pet from the front, the side and the back)
			String[] cubees = {"Cubee Feuer", "Cubee Kanone", "Cubee Bomber-Creeper", "Cubee Glitzerhexe", "Cubee Hoodie-Fuchs", "Cubee TNT",
				"Cubee Baby-Ghast", "Blobfisch", "Otter"};
			ctx.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			for (int ci = 0; ci < cubees.length; ci++) {
				String cubee = cubees[ci];
				sp.getServer().runCommand("tp @a 70 -60 70 0 0");
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.set(cubee));
				ctx.waitTicks(25);
				check(ctx.computeOnClient(mc -> dev.novaclient.cosmetics.PetRenderer.lastSubmits() == 1
					&& dev.novaclient.cosmetics.PetRenderer.lastQuads() > 30), cubee + " not drawn");
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).freezeForTest(true));
				String[] views = {"front", "side", "back"};
				for (int vi = 0; vi < views.length; vi++) {
					final int view = vi;
					String cmd = ctx.computeOnClient(mc -> {
						var b = (net.minecraft.world.entity.LivingEntity) mc.level.getEntity(-4_242_042);
						double yaw = Math.toRadians(b.yBodyRot + view * 90);
						double dist = 1.7;
						double x = b.getX() - Math.sin(yaw) * dist;
						double z = b.getZ() + Math.cos(yaw) * dist;
						double footY = mc.player.getY();
						double dx = b.getX() - x;
						double dz = b.getZ() - z;
						double dy = b.getY() + 0.3 - (footY + mc.player.getEyeHeight());
						float lookYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
						float lookPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
						return String.format(java.util.Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", x, footY, z, lookYaw, lookPitch);
					});
					sp.getServer().runCommand(cmd);
					ctx.waitTicks(6);
					ctx.takeScreenshot("38f_pet_" + ci + "_" + views[vi]);
				}
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).freezeForTest(false));
			}
			ctx.runOnClient(mc -> {
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			sp.getServer().runCommand("tp @a 70 -60 70 0 30");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.set("Cubee Feuer"));
			ctx.waitTicks(15);
			sp.getServer().runCommand("execute as @p at @s run tp @s ~5 ~ ~");
			float maxBlend = 0;
			for (int i = 0; i < 14; i++) {
				ctx.waitTick();
				maxBlend = Math.max(maxBlend, ctx.computeOnClient(mc -> dev.novaclient.cosmetics.PetRenderer.moveBlend()));
				if (i == 5) {
					ctx.runOnClient(mc -> mc.gui.hud.toggle());
					ctx.takeScreenshot("38g_cubee_walking");
					ctx.runOnClient(mc -> mc.gui.hud.toggle());
				}
			}
			check(maxBlend > 0.3, "cubee did not play its walk animation: " + maxBlend);
			ctx.waitTicks(30);
			check(ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_042).getY() - mc.player.getY()) < 0.45, "cubee is not on the ground");
			ctx.takeScreenshot("38h_cubee_after_walk");
			// a walker really walks after you (legs move) when you go away
			ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.set("Kupfergolem"));
			ctx.waitTicks(10);
			sp.getServer().runCommand("execute as @p at @s run tp @s ~4 ~ ~");
			float maxSpeed = 0;
			for (int i = 0; i < 12; i++) {
				ctx.waitTick();
				maxSpeed = Math.max(maxSpeed, ctx.computeOnClient(mc ->
					((net.minecraft.world.entity.LivingEntity) mc.level.getEntity(-4_242_042)).walkAnimation.speed()));
			}
			check(maxSpeed > 0.1, "copper golem did not walk (legs) after the player: walk speed " + maxSpeed);
			ctx.waitTicks(10);
			ctx.takeScreenshot("38c_copper_golem_walked");
			sp.getServer().runCommand("tp @a 40 -60 40 180 0");
			ctx.runOnClient(mc -> {
				module("Begleiter").setEnabled(false);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			ctx.waitTicks(2);
			check(ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_042) == null), "companion not removed when disabled");

			// ---------------------------------------------------------------- waypoint, stopwatch (real keys)
			enable(ctx, "Wegpunkt", "Stoppuhr", "Block-Info", "Inventar-Vorschau", "Spieler online", "Wetter");
			ctx.waitTick();
			input.pressKey(InputConstants.KEY_B);
			ctx.waitTicks(2);
			check(ctx.computeOnClient(mc -> dev.novaclient.world.Waypoints.here(mc).size()) == 1, "waypoint not set by key B");
			// a second one with a name via the client command (typed into the real chat)
			sp.getServer().runCommand("tp @a ~-6 ~ ~");
			ctx.waitTicks(2);
			input.pressKey(InputConstants.KEY_T);
			ctx.waitForScreen(net.minecraft.client.gui.screens.ChatScreen.class);
			input.typeChars("/wegpunkt setzen Basis");
			input.pressKey(InputConstants.KEY_RETURN);
			ctx.waitTicks(3);
			check(ctx.computeOnClient(mc -> dev.novaclient.world.Waypoints.here(mc).stream().anyMatch(w -> w.name().equals("Basis"))),
				"/wegpunkt setzen did not create 'Basis': " + ctx.computeOnClient(mc -> dev.novaclient.world.Waypoints.here(mc)));
			check(ctx.computeOnClient(mc -> readConfig("waypoints.json").contains("Basis")), "waypoints are not saved");
			input.pressKey(InputConstants.KEY_NUMPAD0);
			ctx.waitTicks(25);
			input.pressKey(InputConstants.KEY_NUMPAD0);
			ctx.waitTicks(2);
			long elapsed = ctx.computeOnClient(mc -> ((dev.novaclient.module.hud.StopwatchModule) module("Stoppuhr")).elapsedMillis());
			check(elapsed > 300, "stopwatch did not run: " + elapsed);
			sp.getServer().runCommand("tp @a ~5 ~ ~5");
			ctx.waitTicks(5);
			ctx.takeScreenshot("39_waypoint_stopwatch_blockinfo");

			// ---------------------------------------------------------------- chat macro: edit text in the menu, bind N, press N
			input.pressKey(InputConstants.KEY_RSHIFT);
			ctx.waitForScreen(dev.novaclient.gui.NovaHubScreen.class);
			input.pressKey(InputConstants.KEY_1);
			ctx.waitForScreen(ClickGuiScreen.class);
			input.typeChars("makro");
			ctx.waitTick();
			Module macros = ctx.computeOnClient(mc -> module("Chat-Makros"));
			check(gui(ctx, s -> s.cardCenter(macros)) != null, "search 'makro' did not find Chat-Makros");
			ctx.takeScreenshot("39b_search_makro");
			click(ctx, gui(ctx, s -> s.cardCenter(macros)), RMB);
			click(ctx, gui(ctx, s -> s.rowPoint(1, -1)), LMB);
			for (int i = 0; i < 10; i++) {
				input.pressKey(InputConstants.KEY_BACKSPACE);
			}
			input.typeChars("Hallo Nova");
			input.pressKey(InputConstants.KEY_RETURN);
			ctx.waitTick();
			String macroText = ctx.computeOnClient(mc -> ((dev.novaclient.module.utility.ChatMacrosModule) module("Chat-Makros")).text(0).get());
			check(macroText.equals("Hallo Nova"), "macro text typed in the menu is '" + macroText + "'");
			click(ctx, gui(ctx, s -> s.rowPoint(0, -1)), LMB);
			input.pressKey(InputConstants.KEY_N);
			ctx.waitTick();
			ctx.takeScreenshot("40_macro_settings");
			ctx.runOnClient(mc -> module("Chat-Makros").setEnabled(true));
			ctx.setScreen(() -> null);
			ctx.waitTicks(2);
			input.pressKey(InputConstants.KEY_N);
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> ((dev.novaclient.module.utility.ChatMacrosModule) module("Chat-Makros")).getLastSent()).equals("Hallo Nova"),
				"macro was not sent");
			ctx.takeScreenshot("41_macro_sent");

			// ================================================================ 1.4: network, cosmetics, music, emotes
			Process backend = startBackend();
			try {
				String server = "http://127.0.0.1:" + BACKEND_PORT;
				java.util.UUID me = ctx.computeOnClient(mc -> mc.getUser().getProfileId());
				String myName = ctx.computeOnClient(mc -> mc.getUser().getName());
				String myToken = devToken(server, me, myName);
				java.util.UUID flipp = java.util.UUID.fromString("0c68d1f1-ba5c-4356-833b-5f1cb4e4530c");
				String flippToken = devToken(server, flipp, "Flipp1009");
				ctx.runOnClient(mc -> {
					((dev.novaclient.module.utility.NetworkModule) module("Nova-Netzwerk")).server.set(server);
					module("Nova-Netzwerk").loadEnabled(true);
					dev.novaclient.network.NovaNetwork.setSessionForTest(server, me, myToken);
				});

				// a second Nova player (the owner Flipp1009) standing next to us, on a free spot without helmet
				sp.getServer().runCommand("kill @e[type=minecraft:armor_stand]");
				sp.getServer().runCommand("item replace entity @a armor.head with minecraft:air");
				sp.getServer().runCommand("item replace entity @a armor.chest with minecraft:air");
				sp.getServer().runCommand("tp @a 40 -60 40 180 0");
				ctx.waitTicks(5);
				ctx.runOnClient(mc -> {
					net.minecraft.client.player.RemotePlayer other = new net.minecraft.client.player.RemotePlayer(mc.level,
						new com.mojang.authlib.GameProfile(flipp, "Flipp1009"));
					other.setId(-777);
					other.snapTo(mc.player.getX() + 1.6, mc.player.getY(), mc.player.getZ(), 180, 0);
					other.setYHeadRot(180);
					other.yBodyRot = 180;
					mc.level.addEntity(other);
				});
				pushRemote(server, flippToken, "wave");

				// our own outfit + song
				ctx.runOnClient(mc -> {
					((dev.novaclient.module.cosmetics.HeadCosmeticModule) module("Kopfschmuck")).type.set("Krone");
					((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).type.set("Engelsflügel");
					((dev.novaclient.module.cosmetics.CapeModule) module("Cape")).design.set("Galaxie (animiert)");
					module("Kopfschmuck").setEnabled(true);
					module("Flügel").setEnabled(true);
					module("Cape").setEnabled(true);
					module("Nametags").setEnabled(true);
					module("Musik").setEnabled(true);
					module("Partikel-Spur").setEnabled(false);
					module("Partikel-Ring").setEnabled(false);
					module("Begleiter").setEnabled(false);
					((dev.novaclient.module.cosmetics.HeadCosmeticModule) module("Kopfschmuck")).color.set("Blau");
					((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).mode.set("Kette");
					module("Minimee").setEnabled(true);
					dev.novaclient.music.CoverArt.putForTest("Nova Theme", "DJ Nova", testCover(0x8B5CF6));
					dev.novaclient.music.CoverArt.putForTest("Blinding Lights", "The Weeknd", testCover(0xE53935));
					dev.novaclient.music.NowPlaying.setOverride(new dev.novaclient.music.NowPlaying.Song("Nova Theme", "DJ Nova", true, "Spotify",
						61_000, 200_000, System.currentTimeMillis()));
					dev.novaclient.network.NovaNetwork.requestSync();
				});
				ctx.waitFor(mc -> dev.novaclient.network.NovaNetwork.isNovaUser(flipp), 200);
				dev.novaclient.cosmetics.Loadout seen = ctx.computeOnClient(mc -> dev.novaclient.network.NovaNetwork.loadout(flipp));
				check(seen != null && seen.head().equals("tophat") && seen.wings().equals("dragon") && seen.cape().equals("anim:fire"),
					"remote loadout not received: " + seen);
				check("Blinding Lights – The Weeknd".equals(ctx.computeOnClient(mc -> dev.novaclient.network.NovaNetwork.song(flipp))),
					"remote song not received");
				check(ctx.computeOnClient(mc -> dev.novaclient.cosmetics.Badges.isOwner(flipp)), "Flipp1009 must be an owner");
				check(seen.minimee().equals("EinfxchFiete") && seen.minimeeMode().equals("pocket"), "remote minimee not received: " + seen);
				check(seen.extra().equals("foxtail"), "remote extra not received: " + seen);
				var remoteSong = ctx.computeOnClient(mc -> dev.novaclient.network.NovaNetwork.songInfo(flipp));
				check(remoteSong.lengthMs() == 200_000 && remoteSong.positionNow() >= 83_000 && remoteSong.timestamp().endsWith("/ 3:20"),
					"remote song time wrong: " + remoteSong + " " + remoteSong.timestamp());
				check(ctx.computeOnClient(mc -> dev.novaclient.music.CoverArt.get("Blinding Lights", "The Weeknd")) != null, "cover texture missing");
				String remoteCape = ctx.computeOnClient(mc -> {
					var e = mc.level.getEntity(-777);
					var c = ((net.minecraft.client.player.AbstractClientPlayer) e).getSkin().cape();
					return c == null ? "none" : c.texturePath().toString();
				});
				check(remoteCape.startsWith("novaclient:textures/cape/anim/fire_"), "remote cape not applied: " + remoteCape);

				// front view: crown + song above own name, owner crown + tophat + song + wave on the other player
				ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
				pushRemote(server, flippToken, "wave");
				ctx.waitTicks(20);
				ctx.takeScreenshot("42_network_front");
				// back view: capes and wings
				ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
				pushRemote(server, flippToken, "dance");
				ctx.waitTicks(25);
				ctx.takeScreenshot("43_network_back");
				ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

				// emote wheel with the real key V
				input.pressKey(InputConstants.KEY_V);
				ctx.waitForScreen(dev.novaclient.emote.EmoteWheelScreen.class);
				ctx.waitTicks(2);
				ctx.takeScreenshot("44_emote_wheel");
				int[] waveBtn = ctx.computeOnClient(mc -> ((dev.novaclient.emote.EmoteWheelScreen) mc.gui.screen()).buttonCenter("cheer"));
				click(ctx, waveBtn, LMB);
				check("cheer".equals(ctx.computeOnClient(mc -> {
					var a = dev.novaclient.emote.Emotes.localActive();
					return a == null ? "" : a.id();
				})), "emote from the wheel not playing");
				check(ctx.computeOnClient(mc -> mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT), "emote should turn the camera");
				ctx.waitTicks(15);
				ctx.takeScreenshot("45_emote_cheer");
				ctx.waitTicks(60);
				check(ctx.computeOnClient(mc -> dev.novaclient.emote.Emotes.localActive() == null), "emote should end");
				check(ctx.computeOnClient(mc -> mc.options.getCameraType() == CameraType.FIRST_PERSON), "camera not restored after emote");

				// cosmetics menu with preview, opened from the main menu footer
				input.pressKey(InputConstants.KEY_RSHIFT);
				ctx.waitForScreen(dev.novaclient.gui.NovaHubScreen.class);
				input.pressKey(InputConstants.KEY_1);
				ctx.waitForScreen(ClickGuiScreen.class);
				click(ctx, gui(ctx, s -> s.footerButtonCenter(1)), LMB);
				ctx.waitForScreen(dev.novaclient.gui.CosmeticsScreen.class);
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(200));
				ctx.waitTicks(3);
				ctx.takeScreenshot("46_cosmetics_capes");
				java.util.function.Function<java.util.function.Function<dev.novaclient.gui.CosmeticsScreen, int[]>, int[]> at =
					f -> ctx.computeOnClient(mc -> f.apply((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()));
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).scrollTo("Nova Puls"));
				ctx.waitTick();
				ctx.takeScreenshot("46b_cosmetics_capes_animated");
				click(ctx, at.apply(s -> s.tileCenter("Nova Puls")), LMB);
				check(ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.CapeModule) module("Cape")).design.get()).equals("Nova Puls (animiert)"),
					"cape tile click did not select the animated Nova cape");
				click(ctx, at.apply(s -> s.tabCenter(1)), LMB);
				click(ctx, at.apply(s -> s.tileCenter("Bandana")), LMB);
				click(ctx, at.apply(s -> s.swatchCenter("Lila")), LMB);
				check(ctx.computeOnClient(mc -> {
					var h = (dev.novaclient.module.cosmetics.HeadCosmeticModule) module("Kopfschmuck");
					return h.isEnabled() && h.type.get().equals("Bandana") && h.color.get().equals("Lila");
				}), "head tile/colour click failed");
				ctx.waitTicks(3);
				ctx.takeScreenshot("47_cosmetics_head");
				click(ctx, at.apply(s -> s.tabCenter(2)), LMB);
				click(ctx, at.apply(s -> s.tileCenter("Schmetterling")), LMB);
				check(ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).type.get()).equals("Schmetterling"),
					"wings tile click failed");
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(20));
				ctx.waitTicks(3);
				ctx.takeScreenshot("48_cosmetics_wings_back");
				click(ctx, at.apply(s -> s.swatchCenter("Grün")), LMB);
				check(ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).color.get()).equals("Grün"),
					"wings colour click failed");
				ctx.waitTicks(2);
				ctx.takeScreenshot("48b_wings_green");
				// minimee: type a name (the settings are at the top), then pick a position tile below them
				click(ctx, at.apply(s -> s.tabCenter(3)), LMB);
				ctx.waitTick();
				ctx.takeScreenshot("48c_minimee_tab");
				click(ctx, at.apply(s -> s.rowCenter("Spielername")), LMB);
				input.typeChars("Flipp1009!");
				input.pressKey(InputConstants.KEY_RETURN);
				ctx.waitTick();
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).scrollTo("Hosentasche"));
				ctx.waitTick();
				click(ctx, at.apply(s -> s.tileCenter("Hosentasche")), LMB);
				check(ctx.computeOnClient(mc -> {
					var m = (dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee");
					return m.isEnabled() && m.mode.get().equals("Hosentasche") && m.name.get().equals("Flipp1009") && m.playerName().equals("Flipp1009");
				}), "minimee settings wrong: " + ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).name.get()));
				// the animation row cycles its mode on click
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).scrollTo("Spielername"));
				ctx.waitTick();
				click(ctx, at.apply(s -> s.rowCenter("Animation")), LMB);
				check(!ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).anim.get()).equals("Keine"),
					"minimee animation row click did not change the animation");
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).anim.set("Keine"));
				// every position from the front and from behind
				String[] miniModes = ctx.computeOnClient(mc -> dev.novaclient.cosmetics.CosmeticRegistry.MINIMEE_MODES.keySet().toArray(new String[0]));
				for (String id : miniModes) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).mode.set(dev.novaclient.cosmetics.CosmeticRegistry.MINIMEE_MODES.get(id)));
					for (int yaw : new int[] {200, 20}) {
						ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(yaw));
						ctx.waitTicks(2);
						ctx.takeScreenshot("48d_minimee_" + id + "_" + (yaw == 200 ? "front" : "back"));
					}
				}
				check(ctx.computeOnClient(mc -> dev.novaclient.cosmetics.Loadout.fromJson(dev.novaclient.cosmetics.Cosmetics.local().toJson()).minimeeMode())
					.equals(miniModes[miniModes.length - 1]), "minimee mode lost in the JSON round trip");
				// animations and sizes on the neck chain
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).mode.set("Kette"));
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(200));
				for (String anim : new String[] {"Winken", "Tanzen", "Zappeln", "Jubeln", "Kopfnicken"}) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).anim.set(anim));
					ctx.waitTicks(4);
					ctx.takeScreenshot("48e_minimee_anim_" + dev.novaclient.cosmetics.CosmeticRegistry.MINIMEE_ANIMS.entrySet().stream()
						.filter(e -> e.getValue().equals(anim)).findFirst().orElseThrow().getKey());
				}
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).anim.set("Keine"));
				for (String miniSize : new String[] {"Klein", "Groß"}) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).size.set(miniSize));
					ctx.waitTicks(2);
					ctx.takeScreenshot("48e_minimee_size_" + (miniSize.equals("Groß") ? "big" : "small"));
				}
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee")).size.set("Normal"));
				check(ctx.computeOnClient(mc -> dev.novaclient.cosmetics.Minimees.cachedModels()) <= 7 * 6 * 16 * 2,
					"too many cached minimee models");
				// companion tab
				click(ctx, at.apply(s -> s.tabCenter(4)), LMB);
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).scrollTo("Fuchs"));
				click(ctx, at.apply(s -> s.tileCenter("Fuchs")), LMB);
				check(enabled(ctx, "Begleiter") && ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.CompanionModule) module("Begleiter")).type.get()).equals("Fuchs"),
					"companion tile click failed");
				ctx.takeScreenshot("48f_companion_tab");
				// effects tab: switch the kill effect on with its header toggle
				click(ctx, at.apply(s -> s.tabCenter(5)), LMB);
				ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).scrollTo("Kill-Effekt"));
				click(ctx, at.apply(s -> s.headerToggle("Kill-Effekt")), LMB);
				check(enabled(ctx, "Kill-Effekt"), "kill effect toggle failed");
				ctx.takeScreenshot("48g_effects_tab");
				click(ctx, at.apply(s -> s.tabCenter(2)), LMB);
				// all head cosmetics once, so every model is rendered at least once
				for (var head : dev.novaclient.cosmetics.CosmeticRegistry.HEADS.entrySet()) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.HeadCosmeticModule) module("Kopfschmuck")).type.set(head.getValue()));
					ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(200));
					ctx.waitTicks(2);
					ctx.takeScreenshot("49_head_" + head.getKey());
				}
				// extras tab by clicks, then every extra from behind
				click(ctx, at.apply(s -> s.tabCenter(7)), LMB);
				click(ctx, at.apply(s -> s.tileCenter("Rucksack")), LMB);
				check(enabled(ctx, "Extras") && ctx.computeOnClient(mc -> dev.novaclient.cosmetics.Cosmetics.local().extra()).equals("backpack"),
					"extras tile click failed");
				click(ctx, at.apply(s -> s.swatchCenter("Blau")), LMB);
				check(ctx.computeOnClient(mc -> ((dev.novaclient.module.cosmetics.ExtrasModule) module("Extras")).color.get()).equals("Blau"), "extras colour failed");
				ctx.runOnClient(mc -> {
					module("Flügel").setEnabled(false);
					module("Cape").setEnabled(false);
					module("Minimee").setEnabled(false);
					module("Kopfschmuck").setEnabled(false);
				});
				for (var extra : dev.novaclient.cosmetics.CosmeticRegistry.EXTRAS.entrySet()) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.ExtrasModule) module("Extras")).type.set(extra.getValue()));
					ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(extra.getKey().equals("scarf") ? 200 : extra.getKey().endsWith("tail") ? 100 : 35));
					ctx.waitTicks(2);
					ctx.takeScreenshot("49x_extra_" + extra.getKey());
				}
				ctx.runOnClient(mc -> {
					module("Extras").setEnabled(false);
					module("Flügel").setEnabled(true);
					module("Cape").setEnabled(true);
					module("Kopfschmuck").setEnabled(true);
					((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).color.set("Normal");
				});
				for (var wing : dev.novaclient.cosmetics.CosmeticRegistry.WINGS.entrySet()) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).type.set(wing.getValue()));
					ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(20));
					ctx.waitTicks(2);
					ctx.takeScreenshot("49w_wings_" + wing.getKey());
				}
				// the phoenix from all sides, also with a colour (the tint textures) and with the cape off
				ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).type.set("Phönixflügel"));
				for (int yaw : new int[] {60, 100, 150, 200, 260, 330}) {
					ctx.runOnClient(mc -> ((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(yaw));
					ctx.waitTicks(2);
					ctx.takeScreenshot("49p_phoenix_yaw" + yaw);
				}
				ctx.runOnClient(mc -> {
					((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).color.set("Blau");
					module("Cape").setEnabled(false);
					((dev.novaclient.gui.CosmeticsScreen) mc.gui.screen()).setYaw(20);
				});
				ctx.waitTicks(2);
				ctx.takeScreenshot("49p_phoenix_blue");
				ctx.runOnClient(mc -> {
					((dev.novaclient.module.cosmetics.WingsModule) module("Flügel")).color.set("Normal");
					module("Cape").setEnabled(true);
				});
				ctx.setScreen(() -> null);
				ctx.runOnClient(mc -> module("Begleiter").setEnabled(false));
				ctx.waitTicks(2);

				// tab list with ping in ms
				input.holdKey(o -> o.keyPlayerList);
				ctx.waitTicks(3);
				ctx.takeScreenshot("50_tab_list");
				input.releaseKey(o -> o.keyPlayerList);

				// music HUD
				ctx.takeScreenshot("51_music_hud");
				// real detection (Linux: playerctl). The test run puts a fake playerctl on the PATH.
				if (System.getenv("NOVA_FAKE_PLAYERCTL") != null) {
					ctx.runOnClient(mc -> dev.novaclient.music.NowPlaying.setOverride(null));
					ctx.waitFor(mc -> {
						var song = dev.novaclient.music.NowPlaying.current();
						return song != null && song.title().equals("Fake Song");
					}, 300);
					var detected = ctx.computeOnClient(mc -> dev.novaclient.music.NowPlaying.current());
					check(detected.artist().equals("Fake Artist") && detected.playing() && detected.source().equals("Spotify")
						&& detected.lengthMs() == 200_000 && detected.positionNow() >= 61_000, "playerctl output parsed wrong: " + detected);
				}

				// friends: Flipp1009 sends a request, we accept it in the friends menu (opened with key 3 in the hub)
				ctx.runOnClient(mc -> module("Nova-Netzwerk").loadEnabled(true));
				pushFriendRequest(server, flippToken, me);
				ctx.runOnClient(mc -> dev.novaclient.network.NovaNetwork.requestSync());
				ctx.waitFor(mc -> dev.novaclient.friends.Friends.requests().stream().anyMatch(r -> r.uuid().equals(flipp)), 200);
				input.pressKey(InputConstants.KEY_RSHIFT);
				ctx.waitForScreen(dev.novaclient.gui.NovaHubScreen.class);
				ctx.waitTicks(2);
				ctx.takeScreenshot("52_hub_request");
				input.pressKey(InputConstants.KEY_3);
				ctx.waitForScreen(dev.novaclient.gui.FriendsScreen.class);
				ctx.takeScreenshot("53_friends_request");
				click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.FriendsScreen) mc.gui.screen()).buttonCenter("Flipp1009", "Annehmen")), LMB);
				check(ctx.computeOnClient(mc -> dev.novaclient.friends.Friends.isFriend(flipp)), "accepting the request did not add Flipp1009");
				pushFriendRequest(server, flippToken, me);
				ctx.runOnClient(mc -> dev.novaclient.network.NovaNetwork.requestSync());
				ctx.waitFor(mc -> {
					var st = dev.novaclient.friends.Friends.status(flipp);
					return st != null && st.mutual() && st.online();
				}, 200);
				var fst = ctx.computeOnClient(mc -> dev.novaclient.friends.Friends.status(flipp));
				check(fst.server().equals("play.nova-test.net"), "friend server wrong: " + fst);
				check(ctx.computeOnClient(mc -> ((dev.novaclient.gui.FriendsScreen) mc.gui.screen()).buttonCenter("Flipp1009", "Beitreten")) != null,
					"join button missing");
				ctx.waitTicks(2);
				ctx.takeScreenshot("54_friends_online");
				// typing a name of a player that does not exist
				click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.FriendsScreen) mc.gui.screen()).inputCenter()), LMB);
				input.typeChars("Nobody_xyz_404");
				ctx.waitTick();
				click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.FriendsScreen) mc.gui.screen()).addButtonCenter()), LMB);
				ctx.waitFor(mc -> ((dev.novaclient.gui.FriendsScreen) mc.gui.screen()).getMessage().contains("nicht gefunden"), 600);
				check(ctx.computeOnClient(mc -> mc.player.getName().getString().length() > 0), "player gone");
				// the friend marker in front of the name
				check(ctx.computeOnClient(mc -> dev.novaclient.cosmetics.Badges.decorate(flipp, false, net.minecraft.network.chat.Component.literal("Flipp1009"), true)
					.getString().contains("❤")), "friend heart missing");
				ctx.setScreen(() -> null);
				ctx.runOnClient(mc -> dev.novaclient.friends.Friends.remove(flipp));

				// coordinates share key
				ctx.runOnClient(mc -> {
					var m = (dev.novaclient.module.utility.CoordsShareModule) module("Koordinaten teilen");
					m.key.set(InputConstants.KEY_U);
					m.setEnabled(true);
				});
				ctx.waitTick();
				input.pressKey(InputConstants.KEY_U);
				ctx.waitTicks(3);
				String coordsDebug = ctx.computeOnClient(mc -> "presses=" + dev.novaclient.util.KeyState.presses(InputConstants.KEY_U)
					+ " enabled=" + module("Koordinaten teilen").isEnabled() + " notif=" + module("Benachrichtigungen").isEnabled()
					+ " key=" + ((dev.novaclient.module.utility.CoordsShareModule) module("Koordinaten teilen")).key.code()
					+ " screen=" + mc.gui.screen() + " active=" + dev.novaclient.util.Notifications.active());
				check(ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active().contains("Koordinaten gesendet")),
					"coordinates were not shared: " + coordsDebug);
				ctx.runOnClient(mc -> {
					mc.level.removeEntity(-777, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
					dev.novaclient.music.NowPlaying.setOverride(null);
					module("Musik").setEnabled(false);
					module("Nova-Netzwerk").loadEnabled(false);
				});
			} finally {
				backend.destroy();
			}

			// ================================================================ 1.5: ping, /playtime, kill effect, new modules
			check(ctx.computeOnClient(mc -> dev.novaclient.util.PingTracker.measured()) >= 0, "ping was not measured with ping packets");
			input.pressKey(InputConstants.KEY_T);
			ctx.waitForScreen(net.minecraft.client.gui.screens.ChatScreen.class);
			input.typeChars("/playtime");
			input.pressKey(InputConstants.KEY_RETURN);
			ctx.waitTicks(3);
			check(ctx.computeOnClient(mc -> chatContains(mc, "Spielzeit auf")), "/playtime did not answer");
			long played = ctx.computeOnClient(mc -> dev.novaclient.world.Playtime.currentTotal());
			check(played > 20_000, "play time not counted: " + played);
			ctx.runOnClient(mc -> dev.novaclient.world.Playtime.save());
			check(ctx.computeOnClient(mc -> readConfig("playtime.json").contains("sp:")), "play time not saved");
			ctx.takeScreenshot("55_playtime_command");

			// kill effect: a chicken we hit dies -> lightning (client only)
			ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.KillEffectModule) module("Kill-Effekt")).effect.set("Blitz"));
			sp.getServer().runCommand("execute at @p run summon minecraft:chicken ~2 ~ ~ {NoAI:1b}");
			ctx.waitTicks(5);
			ctx.runOnClient(mc -> {
				var chicken = mc.level.getEntitiesOfClass(net.minecraft.world.entity.animal.chicken.Chicken.class, mc.player.getBoundingBox().inflate(6)).get(0);
				dev.novaclient.util.CombatTracker.onAttack(mc.player, chicken);
			});
			sp.getServer().runCommand("kill @e[type=minecraft:chicken]");
			ctx.waitTicks(3);
			check(ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_043) instanceof net.minecraft.world.entity.LightningBolt), "kill effect did not play");
			ctx.takeScreenshot("56_kill_effect");
			ctx.waitTicks(30);
			check(ctx.computeOnClient(mc -> mc.level.getEntity(-4_242_043) == null), "kill effect lightning was not removed");

			// new HUD modules, hand size, window title
			enable(ctx, "Ingame-Zeit", "Mondphase", "Blickrichtung", "Erfahrung", "Item-Haltbarkeit", "Hand-Größe", "Fenstertitel");
			ctx.waitTicks(3);
			ctx.takeScreenshot("57_new_modules");
			check(ctx.computeOnClient(mc -> dev.novaclient.module.utility.WindowTitleModule.title(mc)).startsWith("Nova Client " + NovaClient.VERSION),
				"window title wrong");
			ctx.runOnClient(mc -> {
				for (String n : new String[] {"Ingame-Zeit", "Mondphase", "Blickrichtung", "Erfahrung", "Item-Haltbarkeit", "Hand-Größe", "Kill-Effekt", "Minimee"}) {
					module(n).setEnabled(false);
				}
			});

			// ================================================================ 1.6: emotes, modules
			for (String emote : new String[] {"bow", "salute", "zombie", "shrug", "think"}) {
				ctx.runOnClient(mc -> dev.novaclient.emote.Emotes.play(emote));
				ctx.waitTicks(8);
				check(emote.equals(ctx.computeOnClient(mc -> {
					var a = dev.novaclient.emote.Emotes.localActive();
					return a == null ? "" : a.id();
				})), "emote " + emote + " not playing");
				ctx.takeScreenshot("58_emote_" + emote);
				ctx.runOnClient(mc -> dev.novaclient.emote.Emotes.stop());
				ctx.waitTicks(2);
			}
			enable(ctx, "Minimap", "Bewegungsstatus", "Hotbar-Nummern", "Schadenszahlen", "Leben über Spielern", "Rückblick", "Chat-Erwähnungen");
			sp.getServer().runCommand("execute at @p run summon minecraft:pig ~1 ~ ~-3 {NoAI:1b}");
			ctx.waitTicks(25);
			sp.getServer().runCommand("damage @e[type=minecraft:pig,limit=1] 3");
			ctx.waitTicks(3);
			check(ctx.computeOnClient(mc -> ((dev.novaclient.module.visual.DamageNumbersModule) module("Schadenszahlen")).activePopups()) > 0,
				"no damage number for the pig");
			ctx.takeScreenshot("59_minimap_damage_hotbar");
			check(ctx.computeOnClient(mc -> {
				var m = (dev.novaclient.module.utility.ChatMentionsModule) module("Chat-Erwähnungen");
				String me = mc.player.getGameProfile().name();
				return m.mentions("hey " + me + " komm mal") && !m.mentions("hey " + me + "xyz") && !m.mentions("hallo zusammen");
			}), "chat mention detection wrong");
			input.holdKey(InputConstants.KEY_H);
			ctx.waitTicks(3);
			check(ctx.computeOnClient(mc -> mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT), "rear view key did not turn the camera");
			ctx.takeScreenshot("60_rear_view");
			input.releaseKey(InputConstants.KEY_H);
			ctx.waitTicks(3);
			check(ctx.computeOnClient(mc -> mc.options.getCameraType() == CameraType.FIRST_PERSON), "rear view did not go back");
			sp.getServer().runCommand("kill @e[type=minecraft:pig]");

			// ================================================================ 1.7: item physics, chat, scoreboard, hit marker
			enable(ctx, "Item-Physik", "Kompakter Chat", "Langer Chat-Verlauf", "Scoreboard-Zahlen aus", "Trefferanzeige");
			ctx.runOnClient(mc -> module("HUD-Tweaks").setEnabled(false));
			sp.getServer().runCommand("execute at @p run summon minecraft:item ~0.8 ~ ~-2.5 {PickupDelay:32767,Item:{id:\"minecraft:diamond_sword\",count:1}}");
			sp.getServer().runCommand("execute at @p run summon minecraft:item ~-0.8 ~ ~-2.5 {PickupDelay:32767,Item:{id:\"minecraft:grass_block\",count:3}}");
			sp.getServer().runCommand("execute at @p run summon minecraft:item ~0 ~ ~-3.5 {PickupDelay:32767,Item:{id:\"minecraft:apple\",count:1}}");
			ctx.runOnClient(mc -> mc.player.setXRot(35));
			sp.getServer().runCommand("scoreboard objectives add novatest dummy \"Nova Test\"");
			sp.getServer().runCommand("scoreboard objectives setdisplay sidebar novatest");
			sp.getServer().runCommand("scoreboard players set Punkte novatest 42");
			ctx.runOnClient(mc -> {
				for (int i = 0; i < 3; i++) {
					mc.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("Doppelte Nachricht"));
				}
				dev.novaclient.module.visual.HitMarkerModule.onHit();
			});
			check(ctx.computeOnClient(mc -> dev.novaclient.module.visual.HitMarkerModule.visible()), "hit marker not shown");
			ctx.waitTicks(30);
			check(ctx.computeOnClient(mc -> chatContains(mc, "Doppelte Nachricht (x3)")), "compact chat did not merge the messages");
			ctx.takeScreenshot("61_item_physics_scoreboard");
			ctx.runOnClient(mc -> module("Item-Physik").setEnabled(false));
			ctx.waitTicks(2);
			ctx.takeScreenshot("61b_items_vanilla");
			ctx.runOnClient(mc -> mc.player.setXRot(0));
			ctx.runOnClient(mc -> {
				for (int i = 0; i < 160; i++) {
					mc.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("Zeile " + i));
				}
			});
			check(ctx.computeOnClient(mc -> chatSize(mc)) > 150, "long chat history not kept: " + ctx.computeOnClient(mc -> chatSize(mc)));
			sp.getServer().runCommand("kill @e[type=minecraft:item]");
			sp.getServer().runCommand("scoreboard objectives remove novatest");
			ctx.runOnClient(mc -> {
				for (String n : new String[] {"Item-Physik", "Kompakter Chat", "Langer Chat-Verlauf", "Scoreboard-Zahlen aus", "Trefferanzeige"}) {
					module(n).setEnabled(false);
				}
			});
			ctx.runOnClient(mc -> {
				for (String n : new String[] {"Minimap", "Bewegungsstatus", "Hotbar-Nummern", "Schadenszahlen", "Leben über Spielern", "Rückblick", "Chat-Erwähnungen"}) {
					module(n).setEnabled(false);
				}
			});

			// ================================================================ 1.8: lyrics in the sky, phoenix and minimee in the world
			{
				var lrc = String.join("\n",
					"[ti:Nova Hymne]",
					"[00:01.00]Willkommen im Nova Client",
					"[00:04.00][00:30.00]Wir singen bis zum Himmel",
					"[00:07.50]♪",
					"[00:16.00]<00:16.00>Dein <00:16.50>Song <00:17.00>steht <00:17.40>jetzt <00:17.80>ganz <00:18.20>oben",
					"[00:21.00]Ende");
				var parsed = dev.novaclient.music.Lyrics.parse(lrc);
				check(parsed.lines().size() == 6, "lrc parse: wrong line count " + parsed.lines());
				check(parsed.lines().get(0).startMs() == 1000 && parsed.lines().get(5).startMs() == 30_000, "lrc parse: times wrong " + parsed.lines());
				check(parsed.lines().get(2).isBreak(), "lrc parse: note line should be a break");
				var words = parsed.lines().get(3);
				check(words.text().equals("Dein Song steht jetzt ganz oben") && words.wordChars() != null && words.wordChars()[1] == 5
					&& words.wordMs()[2] == 17_000, "lrc parse: word timings wrong " + words.text() + " " + java.util.Arrays.toString(words.wordChars()));
				check(dev.novaclient.music.Lyrics.parse("[offset:500]\n[00:02.00]Y").lines().get(0).startMs() == 1500, "lrc offset wrong");
				check(dev.novaclient.music.Lyrics.cleanTitle("Song - Remastered 2011").equals("Song")
					&& dev.novaclient.music.Lyrics.cleanTitle("Song (feat. Someone)").equals("Song")
					&& dev.novaclient.music.Lyrics.firstArtist("A, B & C").equals("A"), "title cleaning wrong");

				ctx.runOnClient(mc -> {
					dev.novaclient.music.Lyrics.putForTest("Nova Hymne", "EinfxchFiete", lrc);
					dev.novaclient.music.CoverArt.putForTest("Nova Hymne", "EinfxchFiete", testCover(0x2196F3));
					var m = dev.novaclient.module.hud.LyricsModule.get();
					m.resetToDefaults();
					m.setEnabled(true);
					if (!mc.gui.hud.isHidden()) { mc.gui.hud.toggle(); }
					mc.options.setCameraType(CameraType.FIRST_PERSON);
					mc.player.setYRot(30);
					mc.player.setXRot(-18);
					dev.novaclient.music.NowPlaying.setOverride(new dev.novaclient.music.NowPlaying.Song("Nova Hymne", "EinfxchFiete", true, "Spotify",
						0, 60_000, System.currentTimeMillis()));
				});
				check(ctx.computeOnClient(mc -> dev.novaclient.music.NowPlaying.status()) != null, "song detection status missing");
				java.util.function.LongConsumer at = ms -> ctx.runOnClient(mc -> dev.novaclient.music.NowPlaying.setOverride(
					new dev.novaclient.music.NowPlaying.Song("Nova Hymne", "EinfxchFiete", true, "Spotify", ms, 60_000, System.currentTimeMillis())));
				ctx.waitFor(mc -> {
					var f = dev.novaclient.module.hud.LyricsModule.get().frame();
					return f != null && f.title() > 0.8F;
				}, 200);
				check(ctx.computeOnClient(mc -> dev.novaclient.music.LyricsSky.lastSubmits()) > 0, "no lyrics submitted to the sky");
				ctx.takeScreenshot("62_lyrics_intro");
				at.accept(2000);
				ctx.waitFor(mc -> {
					var f = dev.novaclient.module.hud.LyricsModule.get().frame();
					return f != null && f.current() == 0 && f.change() >= 1 && f.title() < 0.05F;
				}, 200);
				at.accept(2000);
				ctx.waitTicks(1);
				ctx.takeScreenshot("62b_lyrics_singing");
				at.accept(4050);
				ctx.waitFor(mc -> {
					var f = dev.novaclient.module.hud.LyricsModule.get().frame();
					return f != null && f.current() == 1 && f.leaving() == 0;
				}, 100);
				ctx.takeScreenshot("62c_lyrics_transition");
				at.accept(11_000);
				ctx.waitFor(mc -> {
					var f = dev.novaclient.module.hud.LyricsModule.get().frame();
					return f != null && f.current() < 0 && f.dots() > 0.3F && f.dots() < 0.6F && f.change() >= 1;
				}, 200);
				ctx.takeScreenshot("62d_lyrics_dots");
				ctx.runOnClient(mc -> dev.novaclient.module.hud.LyricsModule.get().color.set("Regenbogen"));
				at.accept(17_150);
				ctx.waitFor(mc -> {
					var f = dev.novaclient.module.hud.LyricsModule.get().frame();
					return f != null && f.current() == 3 && f.change() >= 1;
				}, 200);
				at.accept(17_150);
				float wordFill = ctx.computeOnClient(mc -> {
					var f = dev.novaclient.module.hud.LyricsModule.get().frame();
					var l = f.track().line(3);
					return dev.novaclient.module.hud.LyricsModule.fill(l, 17_150, f.track().nextStart(3));
				});
				check(wordFill > 10 && wordFill < 16, "word timed fill wrong: " + wordFill);
				ctx.waitTicks(1);
				ctx.takeScreenshot("62e_lyrics_words_rainbow");
				// the second time tag of a line
				at.accept(30_500);
				ctx.waitFor(mc -> {
					var f = dev.novaclient.module.hud.LyricsModule.get().frame();
					return f != null && f.current() == 5 && f.track().line(5).text().equals("Wir singen bis zum Himmel");
				}, 200);
				// the text follows the view when you turn around
				ctx.runOnClient(mc -> mc.player.setYRot(150));
				ctx.waitFor(mc -> Math.abs(net.minecraft.util.Mth.wrapDegrees(dev.novaclient.music.LyricsSky.anchorYaw() - 150)) < 20, 600);
				ctx.waitTicks(2);
				ctx.takeScreenshot("62f_lyrics_followed");
				// depth tested and in the HUD
				ctx.runOnClient(mc -> {
					var m = dev.novaclient.module.hud.LyricsModule.get();
					m.color.set("Cover");
					m.throughBlocks.set(false);
					m.hud.set(true);
					if (mc.gui.hud.isHidden()) { mc.gui.hud.toggle(); }
				});
				at.accept(31_200);
				ctx.waitTicks(3);
				check(ctx.computeOnClient(mc -> dev.novaclient.module.hud.LyricsModule.get().getWidth(false)) > 0, "lyrics HUD not shown");
				ctx.takeScreenshot("62g_lyrics_hud_cover");
				// optional: the real LRCLIB lookup (only logged, the test must not depend on the internet)
				ctx.runOnClient(mc -> dev.novaclient.music.Lyrics.state("Blinding Lights", "The Weeknd", 200_000));
				for (int i = 0; i < 100 && ctx.computeOnClient(mc -> dev.novaclient.music.Lyrics.state("Blinding Lights", "The Weeknd", 200_000))
					== dev.novaclient.music.Lyrics.State.LOADING; i++) {
					ctx.waitTicks(5);
				}
				NovaClient.LOGGER.info("NOVA LYRICS LRCLIB " + ctx.computeOnClient(mc -> {
					var t = dev.novaclient.music.Lyrics.get("Blinding Lights", "The Weeknd", 200_000);
					return dev.novaclient.music.Lyrics.state("Blinding Lights", "The Weeknd", 200_000) + " lines="
						+ (t == null ? 0 : t.lines().size()) + (t == null || t.lines().size() < 3 ? "" : " third=" + t.lines().get(2).text());
				}));
				ctx.runOnClient(mc -> {
					dev.novaclient.module.hud.LyricsModule.get().resetToDefaults();
					dev.novaclient.module.hud.LyricsModule.get().setEnabled(false);
					dev.novaclient.music.NowPlaying.setOverride(null);
					mc.player.setXRot(0);
				});
				ctx.waitTicks(2);
				check(ctx.computeOnClient(mc -> dev.novaclient.music.LyricsSky.lastSubmits()) == 0, "lyrics still drawn after switching off");

				// ================================================================ 1.8.4: music menu, lyrics sync and matching
				// clock smoothing: small jitter is corrected slowly, a seek is taken over at once
				{
					long now0 = System.currentTimeMillis();
					var base = new dev.novaclient.music.NowPlaying.Song("A", "B", true, "Spotify", 10_000, 200_000, now0);
					var jitter = new dev.novaclient.music.NowPlaying.Song("A", "B", true, "Spotify", 10_240, 200_000, now0);
					var smooth = dev.novaclient.music.NowPlaying.smoothForTest(base, jitter);
					check(Math.abs(smooth.positionNow() - (base.positionNow() + 60)) < 40, "jitter not smoothed: " + smooth.positionNow());
					var seek = new dev.novaclient.music.NowPlaying.Song("A", "B", true, "Spotify", 90_000, 200_000, now0);
					check(Math.abs(dev.novaclient.music.NowPlaying.smoothForTest(base, seek).positionNow() - 90_000) < 40, "seek not taken over");
					var other = new dev.novaclient.music.NowPlaying.Song("C", "B", true, "Spotify", 10_100, 200_000, now0);
					check(dev.novaclient.music.NowPlaying.smoothForTest(base, other) == other, "new song must not be smoothed");
				}
				// which lyrics fit: title, artist and length must match; video titles are turned into song + artist
				{
					String[] q = dev.novaclient.music.Lyrics.query("The Weeknd - Blinding Lights (Official Video)", "TheWeekndVEVO", "Browser");
					check(q[0].equals("Blinding Lights") && q[1].equals("The Weeknd"), "video title not parsed: " + java.util.Arrays.toString(q));
					String[] q2 = dev.novaclient.music.Lyrics.query("Bohemian Rhapsody - Remastered 2011", "Queen", "Spotify");
					check(q2[0].equals("Bohemian Rhapsody - Remastered 2011") && q2[1].equals("Queen"), "spotify title changed: " + java.util.Arrays.toString(q2));
					String[] q3 = dev.novaclient.music.Lyrics.query("Atemlos durch die Nacht", "Helene Fischer - Topic", "Browser");
					check(q3[0].equals("Atemlos durch die Nacht") && q3[1].equals("Helene Fischer"), "topic channel not parsed: " + java.util.Arrays.toString(q3));
					java.util.function.Function<String[], Double> sc = a -> {
						var o = new com.google.gson.JsonObject();
						o.addProperty("trackName", a[0]);
						o.addProperty("artistName", a[1]);
						o.addProperty("duration", Double.parseDouble(a[2]));
						return dev.novaclient.music.Lyrics.scoreForTest(o, "Mr. Brightside", "The Killers", 222);
					};
					check(!Double.isNaN(sc.apply(new String[] {"Mr Brightside", "The Killers", "223"})), "same song rejected");
					check(Double.isNaN(sc.apply(new String[] {"Mr. Brightside", "Some Cover Band", "222"})), "cover by another artist accepted");
					check(Double.isNaN(sc.apply(new String[] {"Mr. Brightside", "The Killers", "250"})), "other version (length) accepted");
					check(Double.isNaN(sc.apply(new String[] {"Somebody Told Me", "The Killers", "222"})), "other song accepted");
					check(sc.apply(new String[] {"Mr. Brightside", "The Killers", "222"}) > sc.apply(new String[] {"Mr. Brightside", "The Killers", "227"}),
						"closer length must win");
				}
				java.nio.file.Path musicFile = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "nova-music-" + System.nanoTime() + ".json");
				var lrc2 = String.join("\n",
					"[00:02.00]Erste Zeile vom Testsong",
					"[00:06.00]Zweite Zeile kommt hier",
					"[00:10.00]Dritte Zeile zum Klicken",
					"[00:14.00]Vierte Zeile",
					"[00:18.00]Fünfte Zeile",
					"[00:22.00]Sechste Zeile",
					"[00:26.00]Ende vom Lied");
				ctx.runOnClient(mc -> {
					dev.novaclient.music.MusicData.useFileForTest(musicFile);
					dev.novaclient.music.Lyrics.putForTest("Testsong", "Nova Band", lrc2);
					dev.novaclient.music.CoverArt.putForTest("Testsong", "Nova Band", testCover(0x43A047));
					var lm = dev.novaclient.module.hud.LyricsModule.get();
					lm.resetToDefaults();
					lm.setEnabled(true);
					module("Musik").setEnabled(true);
					if (mc.gui.hud.isHidden()) { mc.gui.hud.toggle(); }
					dev.novaclient.music.NowPlaying.setOverride(new dev.novaclient.music.NowPlaying.Song("Testsong", "Nova Band", true, "Spotify",
						7_000, 120_000, System.currentTimeMillis()));
				});
				ctx.waitTicks(10);
				// song change: notification and history
				check(ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active().stream().anyMatch(t -> t.contains("Testsong"))),
					"no notification for the new song");
				check(ctx.computeOnClient(mc -> !dev.novaclient.music.MusicData.recent().isEmpty()
					&& dev.novaclient.music.MusicData.recent().get(0).title().equals("Testsong")), "song not in the history");
				// the next line is shown a little before it is sung
				ctx.runOnClient(mc -> dev.novaclient.music.NowPlaying.setOverride(new dev.novaclient.music.NowPlaying.Song("Testsong", "Nova Band", true,
					"Spotify", 9_850, 120_000, System.currentTimeMillis())));
				check(ctx.computeOnClient(mc -> dev.novaclient.module.hud.LyricsModule.get().frame().current()) == 2, "line not shown early");
				// per-song offset: one "earlier" press = 0.1 s
				long before = ctx.computeOnClient(mc -> dev.novaclient.module.hud.LyricsModule.get().totalOffset(dev.novaclient.music.NowPlaying.current()));
				ctx.runOnClient(mc -> dev.novaclient.module.hud.LyricsModule.nudge(dev.novaclient.music.NowPlaying.current(), 100));
				long after = ctx.computeOnClient(mc -> dev.novaclient.module.hud.LyricsModule.get().totalOffset(dev.novaclient.music.NowPlaying.current()));
				check(after - before == 100, "offset nudge wrong: " + before + " -> " + after);
				// /songtext and /song
				input.pressKey(InputConstants.KEY_T);
				ctx.waitForScreen(net.minecraft.client.gui.screens.ChatScreen.class);
				input.typeChars("/songtext später 0.3");
				input.pressKey(InputConstants.KEY_RETURN);
				ctx.waitTicks(3);
				check(ctx.computeOnClient(mc -> chatContains(mc, "Songtext jetzt 0,2 s später")), "/songtext später did not answer");
				input.pressKey(InputConstants.KEY_T);
				ctx.waitForScreen(net.minecraft.client.gui.screens.ChatScreen.class);
				input.typeChars("/song");
				input.pressKey(InputConstants.KEY_RETURN);
				ctx.waitTicks(3);
				check(ctx.computeOnClient(mc -> chatContains(mc, "Testsong – Nova Band")), "/song did not answer");
				ctx.takeScreenshot("63a_song_commands");
				// the new lyrics styles in the sky
				for (String st : new String[] {"Wort für Wort", "Ganze Zeile"}) {
					ctx.runOnClient(mc -> {
						dev.novaclient.module.hud.LyricsModule.get().style.set(st);
						if (!mc.gui.hud.isHidden()) { mc.gui.hud.toggle(); }
						mc.player.setXRot(-18);
						dev.novaclient.music.NowPlaying.setOverride(new dev.novaclient.music.NowPlaying.Song("Testsong", "Nova Band", true,
							"Spotify", 10_600, 120_000, System.currentTimeMillis()));
					});
					ctx.waitTicks(12);
					ctx.takeScreenshot("63g_lyrics_style_" + (st.startsWith("W") ? "word" : "line"));
				}
				ctx.runOnClient(mc -> {
					dev.novaclient.module.hud.LyricsModule.get().style.set("Karaoke");
					if (mc.gui.hud.isHidden()) { mc.gui.hud.toggle(); }
					mc.player.setXRot(0);
				});

				// the music menu from the start menu (key 4)
				input.pressKey(InputConstants.KEY_RSHIFT);
				ctx.waitForScreen(dev.novaclient.gui.NovaHubScreen.class);
				ctx.takeScreenshot("63b_hub_music_card");
				input.pressKey(InputConstants.KEY_4);
				ctx.waitForScreen(dev.novaclient.gui.MusicScreen.class);
				ctx.waitTicks(10);
				ctx.takeScreenshot("63c_music_lyrics");
				// click the line that is "being sung": the lyrics move to fit
				ctx.runOnClient(mc -> dev.novaclient.music.NowPlaying.setOverride(new dev.novaclient.music.NowPlaying.Song("Testsong", "Nova Band", true,
					"Spotify", 12_000, 120_000, System.currentTimeMillis())));
				ctx.waitTicks(3);
				int[] line4 = ctx.computeOnClient(mc -> ((dev.novaclient.gui.MusicScreen) mc.gui.screen()).lineCenter(4));
				click(ctx, line4, LMB);
				ctx.waitTicks(2);
				int cur = ctx.computeOnClient(mc -> dev.novaclient.music.Lyrics.get(dev.novaclient.music.NowPlaying.current())
					.indexAt(dev.novaclient.music.NowPlaying.current().positionNow()
						+ dev.novaclient.module.hud.LyricsModule.get().totalOffset(dev.novaclient.music.NowPlaying.current())));
				check(cur == 4, "click on a line did not sync the lyrics: current line " + cur);
				ctx.waitTicks(10);
				ctx.takeScreenshot("63d_music_synced");
				// player controls
				click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.MusicScreen) mc.gui.screen()).buttonCenter("⏭")), LMB);
				check(ctx.computeOnClient(mc -> dev.novaclient.music.NowPlaying.lastControl()).equals("NEXT"), "next button sent nothing");
				// switch the lyrics off and on for this song
				click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.MusicScreen) mc.gui.screen()).buttonCenter("Text aus")), LMB);
				check(ctx.computeOnClient(mc -> dev.novaclient.module.hud.LyricsModule.get().frame().hidden()), "lyrics not hidden");
				ctx.waitTicks(2);
				ctx.takeScreenshot("63e_music_hidden");
				click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.MusicScreen) mc.gui.screen()).buttonCenter("Text zeigen")), LMB);
				check(!ctx.computeOnClient(mc -> dev.novaclient.module.hud.LyricsModule.get().frame().hidden()), "lyrics not shown again");
				// statistics: 40 s of listening make one play
				ctx.runOnClient(mc -> {
					dev.novaclient.music.MusicData.listened("Testsong", "Nova Band", 40_000, true);
					dev.novaclient.music.MusicData.played("Blinding Lights", "The Weeknd", "Browser");
				});
				for (String tabName : new String[] {"Verlauf", "Top-Songs", "Mitspieler"}) {
					click(ctx, ctx.computeOnClient(mc -> ((dev.novaclient.gui.MusicScreen) mc.gui.screen()).buttonCenter("tab:" + tabName)), LMB);
					ctx.waitTicks(3);
					ctx.takeScreenshot("63f_music_" + tabName.replace("-", "").toLowerCase(java.util.Locale.ROOT));
				}
				check(ctx.computeOnClient(mc -> ((dev.novaclient.gui.MusicScreen) mc.gui.screen()).tab()) == 3, "tabs do not switch");
				check(ctx.computeOnClient(mc -> dev.novaclient.music.MusicData.top(5).get(0).title()).equals("Testsong"), "top songs wrong");
				input.pressKey(InputConstants.KEY_ESCAPE);
				ctx.waitTicks(3);
				String savedMusic;
				try {
					savedMusic = java.nio.file.Files.readString(musicFile);
				} catch (java.io.IOException e) {
					savedMusic = "";
				}
				check(savedMusic.contains("Testsong"), "music data not saved");
				ctx.runOnClient(mc -> {
					mc.gui.setScreen(null);
					dev.novaclient.module.hud.LyricsModule.get().resetToDefaults();
					dev.novaclient.module.hud.LyricsModule.get().setEnabled(false);
					module("Musik").setEnabled(false);
					dev.novaclient.music.NowPlaying.setOverride(null);
				});
				ctx.waitTicks(2);
				// real LRCLIB lookups with the new matching (only logged, the test must not depend on the internet)
				String[][] real = {{"Mr. Brightside", "The Killers", "Spotify", "222000"}, {"Bohemian Rhapsody - Remastered 2011", "Queen", "Spotify", "354000"},
					{"The Weeknd - Blinding Lights (Official Video)", "TheWeekndVEVO", "Browser", "201000"}, {"Atemlos durch die Nacht", "Helene Fischer", "Spotify", "219000"}};
				for (String[] r : real) {
					var song = new dev.novaclient.music.NowPlaying.Song(r[0], r[1], true, r[2], 0, Long.parseLong(r[3]), System.currentTimeMillis());
					ctx.runOnClient(mc -> dev.novaclient.music.Lyrics.state(song));
					for (int i = 0; i < 100 && ctx.computeOnClient(mc -> dev.novaclient.music.Lyrics.state(song)) == dev.novaclient.music.Lyrics.State.LOADING; i++) {
						ctx.waitTicks(5);
					}
					NovaClient.LOGGER.info("NOVA LYRICS MATCH " + r[0] + " / " + r[1] + " -> " + ctx.computeOnClient(mc ->
						dev.novaclient.music.Lyrics.state(song) + " " + dev.novaclient.music.Lyrics.info(song)));
				}

				// phoenix wings with flames and the piggyback minimee on the cape, in the world from behind
				ctx.runOnClient(mc -> {
					var wings = (dev.novaclient.module.cosmetics.WingsModule) module("Flügel");
					wings.type.set("Phönixflügel");
					wings.color.set("Normal");
					wings.setEnabled(true);
					module("Cape").setEnabled(true);
					var mini = (dev.novaclient.module.cosmetics.MinimeeModule) module("Minimee");
					mini.mode.set("Huckepack");
					mini.setEnabled(true);
					if (!mc.gui.hud.isHidden()) { mc.gui.hud.toggle(); }
					mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				});
				ctx.waitTicks(30);
				ctx.takeScreenshot("63_phoenix_minimee_world");
				input.holdKey(o -> o.keyUp);
				ctx.waitTicks(12);
				ctx.takeScreenshot("63b_phoenix_minimee_walking");
				input.releaseKey(o -> o.keyUp);
				ctx.runOnClient(mc -> {
					module("Flügel").setEnabled(false);
					module("Minimee").setEnabled(false);
					if (mc.gui.hud.isHidden()) { mc.gui.hud.toggle(); }
					mc.options.setCameraType(CameraType.FIRST_PERSON);
				});
				ctx.waitTicks(2);
			}

			// ================================================================ 1.8: FPS boost, new modules, new emotes and capes
			{
				// FPS boost: far entities, far block entities and too many particles are skipped
				check(enabled(ctx, "FPS-Boost"), "FPS boost should be on by default");
				// the shortest entity distance, so the "far" pig is still inside the (small) tracking range of the test world
				ctx.runOnClient(mc -> dev.novaclient.module.visual.FpsBoostModule.get().entityDistance.set(16.0));
				sp.getServer().runCommand("execute at @p run summon minecraft:pig ~30 ~ ~ {NoAI:1b,Tags:[\"novafar\"]}");
				sp.getServer().runCommand("execute at @p run summon minecraft:pig ~5 ~ ~ {NoAI:1b,Tags:[\"novanear\"]}");
				sp.getServer().runCommand("execute at @p run setblock ~56 ~ ~ minecraft:chest");
				sp.getServer().runCommand("execute at @p run setblock ~6 ~ ~2 minecraft:chest");
				ctx.waitTicks(20);
				String cull = ctx.computeOnClient(mc -> {
					var cam = mc.gameRenderer.mainCamera().position();
					String far = "?";
					String near = "?";
					for (var e : mc.level.entitiesForRendering()) {
						if (e instanceof net.minecraft.world.entity.animal.pig.Pig) {
							boolean draw = dev.novaclient.module.visual.FpsBoostModule.shouldDraw(e, cam.x, cam.y, cam.z);
							if (e.distanceToSqr(mc.player) > 20 * 20) {
								far = String.valueOf(draw);
							} else {
								near = String.valueOf(draw);
							}
						}
					}
					var bp = mc.player.blockPosition();
					var farChest = mc.level.getBlockEntity(bp.offset(56, 0, 0));
					var nearChest = mc.level.getBlockEntity(bp.offset(6, 0, 2));
					var d = mc.getBlockEntityRenderDispatcher();
					return far + " " + near + " " + (farChest == null ? "none" : String.valueOf(d.tryExtractRenderState(farChest, 0, null, false) != null))
						+ " " + (nearChest == null ? "none" : String.valueOf(d.tryExtractRenderState(nearChest, 0, null, false) != null));
				});
				check(cull.equals("false true false true"), "FPS boost culling wrong (far pig, near pig, far chest, near chest): " + cull);
				ctx.runOnClient(mc -> {
					var boost = dev.novaclient.module.visual.FpsBoostModule.get();
					boost.maxParticles.set(500.0);
					for (int i = 0; i < 3000; i++) {
						mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, mc.player.getX(), mc.player.getY() + 1, mc.player.getZ(), 0, 0.01, 0);
					}
				});
				ctx.waitTicks(3);
				String particles = ctx.computeOnClient(mc -> mc.particleEngine.countParticles());
				int total = Integer.parseInt(particles.substring(particles.lastIndexOf(' ') + 1));
				check(total <= 1000, "particle limit not applied: " + particles);
				ctx.runOnClient(mc -> dev.novaclient.module.visual.FpsBoostModule.get().resetToDefaults());
				sp.getServer().runCommand("kill @e[type=minecraft:pig]");
				sp.getServer().runCommand("execute at @p run setblock ~56 ~ ~ minecraft:air");
				sp.getServer().runCommand("execute at @p run setblock ~6 ~ ~2 minecraft:air");

				// no fog
				check(ctx.computeOnClient(mc -> {
					module("Kein Nebel").setEnabled(true);
					var fog = new net.minecraft.client.renderer.fog.FogData();
					fog.renderDistanceStart = 100;
					fog.environmentalEnd = 50;
					dev.novaclient.module.visual.NoFogModule.apply(fog, mc.player, true);
					var water = new net.minecraft.client.renderer.fog.FogData();
					water.environmentalEnd = 20;
					dev.novaclient.module.visual.NoFogModule.apply(water, mc.player, false);
					module("Kein Nebel").setEnabled(false);
					return fog.renderDistanceStart > 1.0E6F && fog.environmentalEnd > 1.0E6F && water.environmentalEnd == 20;
				}), "no fog module wrong");

				// TPS from the time packets
				ctx.runOnClient(mc -> module("TPS").setEnabled(true));
				ctx.waitFor(mc -> dev.novaclient.util.TpsTracker.tps() > 0, 400);
				float tps = ctx.computeOnClient(mc -> dev.novaclient.util.TpsTracker.tps());
				NovaClient.LOGGER.info("NOVA TPS " + tps);
				check(tps > 1 && tps <= 20.01F, "TPS out of range: " + tps);

				// session statistics count walking but not teleports
				ctx.runOnClient(mc -> {
					module("Sitzungsstatistik").setEnabled(true);
					mc.player.setYRot(0);
					mc.player.setXRot(0);
				});
				ctx.waitTicks(2);
				double before = ctx.computeOnClient(mc -> ((dev.novaclient.module.hud.SessionStatsModule) module("Sitzungsstatistik")).distance());
				sp.getServer().runCommand("execute as @p at @s run tp @s ~30 ~ ~");
				ctx.waitTicks(3);
				input.holdKey(o -> o.keyUp);
				ctx.waitTicks(20);
				input.releaseKey(o -> o.keyUp);
				ctx.waitTicks(2);
				double walked = ctx.computeOnClient(mc -> ((dev.novaclient.module.hud.SessionStatsModule) module("Sitzungsstatistik")).distance()) - before;
				check(walked > 1 && walked < 15, "session distance wrong (teleport counted?): " + walked);
				ctx.takeScreenshot("64_tps_session_hud");

				// break reminder
				ctx.runOnClient(mc -> {
					var b = (dev.novaclient.module.utility.BreakReminderModule) module("Pausen-Erinnerung");
					b.minutes.set(15.0);
					b.setEnabled(true);
					b.addPlayedForTest(15 * 60_000L);
				});
				ctx.waitTicks(3);
				check(ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active().stream().anyMatch(t -> t.startsWith("Zeit für eine Pause"))),
					"break reminder not shown: " + ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active()));

				// the new emotes and capes in the world
				ctx.runOnClient(mc -> {
					if (!mc.gui.hud.isHidden()) {
						mc.gui.hud.toggle();
					}
					mc.player.setYRot(0);
					mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
					((dev.novaclient.module.cosmetics.EmotesModule) module("Emotes")).switchCamera.set(false);
				});
				for (String emote : new String[] {"headbang", "airguitar", "flex", "panic"}) {
					ctx.runOnClient(mc -> dev.novaclient.emote.Emotes.play(emote));
					ctx.waitTicks(8);
					check(emote.equals(ctx.computeOnClient(mc -> {
						var a = dev.novaclient.emote.Emotes.localActive();
						return a == null ? "" : a.id();
					})), "emote not playing: " + emote);
					ctx.takeScreenshot("65_emote_" + emote);
				}
				// 1.8.5: emotes that move the whole body, new hats and extras
				ctx.runOnClient(mc -> {
					module("Cape").setEnabled(true);
					module("Kopfschmuck").setEnabled(true);
					module("Flügel").setEnabled(false);
					module("Extras").setEnabled(true);
				});
				String[] newHeads = {"Doktorhut", "Irokese", "Cyber-Visier", "Quietsche-Ente", "Maske"};
				String[] newEmotes = {"dab", "sit", "meditate", "sleep", "jumpingjack", "tpose", "robot", "laugh", "cry", "disco", "spin"};
				for (int i = 0; i < newEmotes.length; i++) {
					String emote = newEmotes[i];
					String head = newHeads[i % newHeads.length];
					ctx.runOnClient(mc -> {
						((dev.novaclient.module.cosmetics.HeadCosmeticModule) module("Kopfschmuck")).type.set(head);
						dev.novaclient.emote.Emotes.play(emote);
					});
					ctx.waitTicks(10);
					check(emote.equals(ctx.computeOnClient(mc -> {
						var a = dev.novaclient.emote.Emotes.localActive();
						return a == null ? "" : a.id();
					})), "emote not playing: " + emote);
					ctx.takeScreenshot("66_emote_" + emote);
				}
				check(dev.novaclient.emote.Emotes.ALL.size() == 26, "expected 26 emotes: " + dev.novaclient.emote.Emotes.ALL.size());
				ctx.runOnClient(mc -> {
					dev.novaclient.emote.Emotes.stop();
					mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
					module("Cape").setEnabled(false);
				});
				for (String extra : new String[] {"Rückenschild", "Köcher", "Luftballon"}) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.ExtrasModule) module("Extras")).type.set(extra));
					ctx.waitTicks(4);
					ctx.takeScreenshot("66b_extra_" + extra.substring(0, 2).toLowerCase(java.util.Locale.ROOT).replace("ü", "ue").replace("ö", "oe"));
				}
				ctx.runOnClient(mc -> {
					mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
					((dev.novaclient.module.cosmetics.ExtrasModule) module("Extras")).type.set("Luftballon");
				});
				for (String head : newHeads) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.HeadCosmeticModule) module("Kopfschmuck")).type.set(head));
					ctx.waitTicks(4);
					ctx.takeScreenshot("66c_head_" + head.substring(0, 3).toLowerCase(java.util.Locale.ROOT));
				}
				// the emote wheel has pages now
				ctx.runOnClient(mc -> mc.gui.setScreen(new dev.novaclient.emote.EmoteWheelScreen()));
				ctx.waitTicks(3);
				ctx.takeScreenshot("66d_wheel_page1");
				ctx.runOnClient(mc -> ((dev.novaclient.emote.EmoteWheelScreen) mc.gui.screen()).turnPage(2));
				ctx.waitTicks(3);
				ctx.takeScreenshot("66d_wheel_page3");
				ctx.runOnClient(mc -> mc.gui.setScreen(null));
				// 1.8.6: animated name styles
				var tags = (dev.novaclient.module.cosmetics.NametagModule) module("Nametags");
				ctx.runOnClient(mc -> {
					tags.setEnabled(true);
					tags.color.set("Pink");
					tags.bold.set(true);
					// name tags are only drawn while the HUD is visible
					if (mc.gui.hud.isHidden()) {
						mc.gui.hud.toggle();
					}
					module("Extras").setEnabled(false);
				});
				for (String fx : new String[] {"Farbverlauf", "Regenbogen-Welle", "Metallic", "Pulsieren", "Schimmer", "Blinken", "Chromatisch", "Feuer",
					"Leuchten", "Buchstabe für Buchstabe", "Gold-Stern"}) {
					ctx.runOnClient(mc -> tags.effect.set(fx));
					ctx.waitTicks(4);
					String shown = ctx.computeOnClient(mc -> dev.novaclient.module.cosmetics.NametagModule.colorize(net.minecraft.network.chat.Component.literal("Tester")).getString());
					check(shown.endsWith("Tester") && shown.startsWith("★") == fx.equals("Gold-Stern"), "name effect broke the name: " + fx + " -> " + shown);
					ctx.takeScreenshot("67_name_" + fx.substring(0, 4).toLowerCase(java.util.Locale.ROOT));
				}
				// only the user name is styled: a rank prefix and suffix keep their own colours; also used in the tab list
				String styledParts = ctx.computeOnClient(mc -> {
					tags.effect.set("Feuer");
					var in = net.minecraft.network.chat.Component.literal("[Admin] ").withColor(0xFF0000)
						.append(net.minecraft.network.chat.Component.literal("Tester").withColor(0x00FF00))
						.append(net.minecraft.network.chat.Component.literal(" ❤").withColor(0x0000FF));
					var out = dev.novaclient.module.cosmetics.NametagModule.colorize(in, "Tester");
					StringBuilder sb = new StringBuilder(out.getString()).append('|');
					for (var part : out.getSiblings()) {
						var col = part.getStyle().getColor();
						int rgb = col == null ? -1 : col.getValue();
						sb.append(rgb == 0xFF0000 ? 'p' : rgb == 0x0000FF ? 's' : rgb == 0x00FF00 ? 'x' : 'n');
					}
					var me = mc.getConnection().getPlayerInfo(mc.player.getUUID());
					var tab = mc.gui.hud.getTabList().getNameForDisplay(me);
					sb.append('|').append(tab.toFlatList().size() > 3);
					return sb.toString();
				});
				check(styledParts.equals("[Admin] Tester ❤|ppppppppnnnnnnss|true"), "name effect must only style the user name: " + styledParts);
				ctx.runOnClient(mc -> {
					tags.effect.set("Keiner");
					tags.bold.set(false);
					tags.color.set("Normal");
					if (!mc.gui.hud.isHidden()) {
						mc.gui.hud.toggle();
					}
					module("Extras").setEnabled(true);
				});
				ctx.runOnClient(mc -> {
					dev.novaclient.emote.Emotes.stop();
					module("Kopfschmuck").setEnabled(false);
					((dev.novaclient.module.cosmetics.EmotesModule) module("Emotes")).switchCamera.set(true);
					mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
					module("Cape").setEnabled(true);
					module("Flügel").setEnabled(false);
					module("Extras").setEnabled(false);
				});
				for (String cape : new String[] {"Feuerwerk (animiert)", "Herbstlaub (animiert)", "Blubberblasen (animiert)", "Sternschnuppen (animiert)", "Aquarium (animiert)", "Wolken (animiert)", "Schaltkreis (animiert)", "Lavalampe (animiert)"}) {
					ctx.runOnClient(mc -> ((dev.novaclient.module.cosmetics.CapeModule) module("Cape")).design.set(cape));
					ctx.waitTicks(4);
					ctx.takeScreenshot("65b_cape_" + cape.substring(0, 4).toLowerCase(java.util.Locale.ROOT).replace("ü", "ue"));
				}
				// jetpack flames while in the air
				ctx.runOnClient(mc -> {
					var ex = (dev.novaclient.module.cosmetics.ExtrasModule) module("Extras");
					ex.type.set("Jetpack");
					ex.setEnabled(true);
					module("Cape").setEnabled(false);
				});
				int flamesBefore = ctx.computeOnClient(mc -> dev.novaclient.cosmetics.WingParticles.jetpackFlames());
				input.holdKey(o -> o.keyJump);
				ctx.waitFor(mc -> !mc.player.onGround(), 40);
				ctx.waitTicks(3);
				ctx.takeScreenshot("65c_jetpack_jump");
				input.releaseKey(o -> o.keyJump);
				ctx.waitTicks(10);
				check(ctx.computeOnClient(mc -> dev.novaclient.cosmetics.WingParticles.jetpackFlames()) > flamesBefore, "no jetpack flames while jumping");
				ctx.runOnClient(mc -> {
					module("Extras").setEnabled(false);
					module("Cape").setEnabled(true);
					if (mc.gui.hud.isHidden()) {
						mc.gui.hud.toggle();
					}
					mc.options.setCameraType(CameraType.FIRST_PERSON);
				});
				// 1.8.1: chat filter and hunger warning
				ctx.runOnClient(mc -> {
					var f = (dev.novaclient.module.utility.ChatFilterModule) module("Chat-Filter");
					f.words.set("werbung, spam");
					f.setEnabled(true);
					mc.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("Kauft Ränge! SPAM SPAM"));
					mc.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("Hallo Nova Filter"));
				});
				ctx.waitTicks(2);
				check(ctx.computeOnClient(mc -> chatContains(mc, "Hallo Nova Filter") && !chatContains(mc, "Kauft Ränge")
					&& ((dev.novaclient.module.utility.ChatFilterModule) module("Chat-Filter")).hidden() >= 1), "chat filter wrong");
				ctx.runOnClient(mc -> module("Chat-Filter").setEnabled(false));
				ctx.runOnClient(mc -> module("Hunger-Warnung").setEnabled(true));
				sp.getServer().runCommand("gamemode survival @a");
				sp.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).getFoodData().setFoodLevel(4));
				ctx.waitFor(mc -> mc.player.getFoodData().getFoodLevel() == 4, 100);
				ctx.waitTicks(3);
				check(ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active().stream().anyMatch(t -> t.startsWith("Du hast Hunger"))),
					"hunger warning not shown: " + ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active()));
				sp.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).getFoodData().setFoodLevel(20));
				ctx.runOnClient(mc -> module("Hunger-Warnung").setEnabled(false));

				// totem pops: a real totem saves you, the packet is counted
				ctx.runOnClient(mc -> module("Totem-Pops").setEnabled(true));
				sp.getServer().runCommand("effect clear @a");
				sp.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:totem_of_undying");
				ctx.waitTicks(3);
				sp.getServer().runCommand("damage @p 1000 minecraft:generic");
				ctx.waitFor(mc -> ((dev.novaclient.module.utility.TotemPopsModule) module("Totem-Pops")).pops(mc.player.getUUID()) == 1, 100);
				check(ctx.computeOnClient(mc -> dev.novaclient.util.Notifications.active().stream().anyMatch(t -> t.contains("Totem verbraucht (1)"))),
					"totem notification missing");
				ctx.takeScreenshot("64b_totem_pop");
				sp.getServer().runCommand("effect clear @a");
				sp.getServer().runCommand("effect give @a minecraft:instant_health 1 10");

				ctx.runOnClient(mc -> {
					for (String n : new String[] {"TPS", "Sitzungsstatistik", "Pausen-Erinnerung", "Totem-Pops"}) {
						module(n).setEnabled(false);
					}
				});
			}

			// ---------------------------------------------------------------- config round trip incl. mode setting
			ctx.runOnClient(mc -> {
				CrosshairModule c = (CrosshairModule) module("Custom Crosshair");
				c.style.set("Quadrat");
				NovaClient.config().save();
				HudModule f = (HudModule) module("FPS");
				String ax = f.getAnchorX().name();
				int ox = f.getOffsetX();
				f.setPosition("START", 0, "START", 0);
				c.style.set("Punkt");
				NovaClient.config().reload();
				check(f.getAnchorX().name().equals(ax) && f.getOffsetX() == ox, "config did not restore FPS position");
				check(c.style.get().equals("Quadrat"), "config did not restore crosshair style");
				ModeSetting mode = c.style;
				check(mode.getModes().contains(mode.get()), "invalid mode after load");
			});
			// ---------------------------------------------------------------- death coordinates (last, the player dies)
			sp.getServer().runCommand("gamerule show_death_messages true");
			sp.getServer().runCommand("kill @a");
			ctx.waitTicks(20);
			ctx.takeScreenshot("36_death_coords");
			// auto respawn (1.8.1): the death screen closes by itself
			ctx.runOnClient(mc -> module("Auto-Respawn").setEnabled(true));
			ctx.waitFor(mc -> !(mc.gui.screen() instanceof net.minecraft.client.gui.screens.DeathScreen) && mc.player != null && mc.player.isAlive(), 400);
			ctx.runOnClient(mc -> module("Auto-Respawn").setEnabled(false));
		}
		NovaClient.LOGGER.info("NOVA GAMETEST PASSED");
	}
}
