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
import dev.novaclient.module.cosmetics.HitParticlesModule;
import dev.novaclient.module.visual.FreelookModule;
import dev.novaclient.util.CombatTracker;
import dev.novaclient.util.KeyState;
import dev.novaclient.util.Notifications;
import dev.novaclient.util.PingTracker;
import dev.novaclient.world.Playtime;
import dev.novaclient.command.NovaCommands;
import dev.novaclient.module.hud.WaypointModule;
import dev.novaclient.util.Render;
import dev.novaclient.module.hud.ThemeModule;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import dev.novaclient.cosmetics.CosmeticsLayer;
import dev.novaclient.network.NovaNetwork;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
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
	public static final String VERSION = "1.8.6";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private static ModuleManager modules;
	private static ConfigManager config;
	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		modules = new ModuleManager();
		config = new ConfigManager(modules);
		config.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openMenuKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.novaclient.menu", InputConstants.KEY_RSHIFT, category));

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide() && player instanceof LocalPlayer local) {
				CombatTracker.onAttack(local, entity);
				HitParticlesModule.onHit(entity);
				dev.novaclient.module.visual.HitMarkerModule.onHit();
			}
			return InteractionResult.PASS;
		});

		ClientTickEvents.END_CLIENT_TICK.register(NovaClient::onTick);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				@SuppressWarnings("unchecked")
				RenderLayerParent<AvatarRenderState, PlayerModel> parent = (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) avatar;
				helper.register(new CosmeticsLayer(parent));
			}
		});
		NovaNetwork.start();
		NovaCommands.register();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			dev.novaclient.music.MusicData.saveIfNeeded(true);
			config.save();
			Playtime.save();
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), (graphics, deltaTracker) -> renderHud(graphics));
		// song lyrics in the sky
		net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(dev.novaclient.music.LyricsSky::submit);
		net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(dev.novaclient.cosmetics.PetRenderer::submit);
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
				mc.gui.setScreen(new dev.novaclient.gui.NovaHubScreen());
			}
		}
		if (!mc.isWindowActive()) {
			// key release events are lost while the window is not focused
			KeyState.releaseAll();
		}
		PingTracker.tick(mc);
		dev.novaclient.cosmetics.WingParticles.tick(mc);
		Playtime.tick(mc);
		modules.tick(mc);
		NovaNetwork.clientTick(mc);
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
		HudModule.nextFrame();
		WaypointModule.renderMarkers(graphics, w, h);
		dev.novaclient.module.visual.DamageNumbersModule.render(graphics, w, h);
		dev.novaclient.module.hud.HotbarNumbersModule.render(graphics, w, h);
		dev.novaclient.module.visual.HitMarkerModule.render(graphics, w, h);
		for (HudModule m : modules.getHudModules()) {
			if (m.isEnabled()) {
				m.render(graphics, w, h, false);
			}
		}
		if (FreelookModule.showIndicator()) {
			String text = "Freelook";
			int tw = mc.font.width(text) + 12;
			int x = (w - tw) / 2;
			Render.rounded(graphics, x, h / 2 - 34, x + tw, h / 2 - 22, 0xA0101016);
			graphics.text(mc.font, text, x + 6, h / 2 - 32, ThemeModule.accent(), false);
		}
		Notifications.render(graphics);
	}

	public static ModuleManager modules() {
		return modules;
	}

	public static ConfigManager config() {
		return config;
	}
}
