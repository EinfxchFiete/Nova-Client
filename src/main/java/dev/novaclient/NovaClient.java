package dev.novaclient;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.config.ConfigManager;
import dev.novaclient.gui.ClickGuiScreen;
import dev.novaclient.gui.HudEditorScreen;
import dev.novaclient.module.ModuleManager;
import dev.novaclient.module.hud.HudModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import dev.novaclient.module.visual.CrosshairModule;
import dev.novaclient.module.visual.HudTweaksModule;
import dev.novaclient.util.CombatTracker;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaClient implements ClientModInitializer {
	public static final String MOD_ID = "novaclient";
	public static final String NAME = "Nova Client";
	public static final String VERSION = "1.1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private static ModuleManager modules;
	private static ConfigManager config;
	private static KeyMapping openMenuKey;
	private static KeyMapping zoomKey;
	private static KeyMapping freelookKey;

	@Override
	public void onInitializeClient() {
		modules = new ModuleManager();
		config = new ConfigManager(modules);
		config.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openMenuKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.novaclient.menu", InputConstants.KEY_RSHIFT, category));
		zoomKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.novaclient.zoom", InputConstants.KEY_C, category));
		modules.zoom.setKey(zoomKey);
		freelookKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.novaclient.freelook", InputConstants.KEY_LALT, category));
		modules.freelook.setKey(freelookKey);

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide() && player instanceof LocalPlayer local) {
				CombatTracker.onAttack(local, entity);
			}
			return InteractionResult.PASS;
		});

		ClientTickEvents.END_CLIENT_TICK.register(NovaClient::onTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), (graphics, deltaTracker) -> renderHud(graphics));
		HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, original -> (graphics, deltaTracker) -> {
			if (!(modules.potionEffects.isEnabled() && modules.potionEffects.hideVanilla.isOn())) {
				original.extractRenderState(graphics, deltaTracker);
			}
		});

		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
			Minecraft mc = Minecraft.getInstance();
			if (CrosshairModule.active() && mc.player != null && !mc.player.isSpectator()
				&& !mc.gui.hud.getDebugOverlay().showDebugScreen()) {
				if (mc.options.getCameraType().isFirstPerson()) {
					CrosshairModule.get().render(graphics);
				}
			} else {
				original.extractRenderState(graphics, deltaTracker);
			}
		});
		HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, original -> (graphics, deltaTracker) -> {
			if (!HudTweaksModule.hideScoreboard()) {
				original.extractRenderState(graphics, deltaTracker);
			}
		});
		HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, original -> (graphics, deltaTracker) -> {
			if (!HudTweaksModule.hideBossBar()) {
				original.extractRenderState(graphics, deltaTracker);
			}
		});
		HudElementRegistry.replaceElement(VanillaHudElements.TITLE_AND_SUBTITLE, original -> (graphics, deltaTracker) -> {
			if (!HudTweaksModule.hideTitles()) {
				original.extractRenderState(graphics, deltaTracker);
			}
		});

		LOGGER.info("{} {} loaded with {} modules", NAME, VERSION, modules.getModules().size());
	}

	private static void onTick(Minecraft mc) {
		while (openMenuKey.consumeClick()) {
			if (mc.gui.screen() == null) {
				mc.gui.setScreen(new ClickGuiScreen(null));
			}
		}
		modules.tick(mc);
	}

	private static void renderHud(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() instanceof HudEditorScreen) {
			return;
		}
		if (mc.gui.hud.getDebugOverlay().showDebugScreen()) {
			return;
		}
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		for (HudModule m : modules.getHudModules()) {
			if (m.isEnabled()) {
				m.render(graphics, w, h, false);
			}
		}
	}

	public static ModuleManager modules() {
		return modules;
	}

	public static ConfigManager config() {
		return config;
	}
}
