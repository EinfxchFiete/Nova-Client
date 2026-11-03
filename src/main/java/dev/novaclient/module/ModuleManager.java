package dev.novaclient.module;

import dev.novaclient.module.hud.ArmorStatusModule;
import dev.novaclient.module.hud.ArrowCounterModule;
import dev.novaclient.module.hud.BiomeModule;
import dev.novaclient.module.hud.ComboModule;
import dev.novaclient.module.hud.ReachModule;
import dev.novaclient.module.hud.SessionTimeModule;
import dev.novaclient.module.hud.ClockModule;
import dev.novaclient.module.hud.CoordinatesModule;
import dev.novaclient.module.hud.CpsModule;
import dev.novaclient.module.hud.DayCounterModule;
import dev.novaclient.module.hud.FpsModule;
import dev.novaclient.module.hud.HudModule;
import dev.novaclient.module.hud.KeystrokesModule;
import dev.novaclient.module.hud.MemoryModule;
import dev.novaclient.module.hud.PingModule;
import dev.novaclient.module.hud.PotionEffectsModule;
import dev.novaclient.module.hud.ServerAddressModule;
import dev.novaclient.module.hud.SpeedModule;
import dev.novaclient.module.hud.ThemeModule;
import dev.novaclient.util.CombatTracker;
import dev.novaclient.util.Notifications;
import dev.novaclient.module.cosmetics.AuraModule;
import dev.novaclient.module.cosmetics.MinimeeModule;
import dev.novaclient.module.cosmetics.ExtrasModule;
import dev.novaclient.module.cosmetics.CapeModule;
import dev.novaclient.module.cosmetics.EmotesModule;
import dev.novaclient.module.cosmetics.HeadCosmeticModule;
import dev.novaclient.module.cosmetics.WingsModule;
import dev.novaclient.module.hud.MusicModule;
import dev.novaclient.module.utility.CoordsShareModule;
import dev.novaclient.module.utility.NetworkModule;
import dev.novaclient.module.utility.TabPingModule;
import dev.novaclient.module.cosmetics.CompanionModule;
import dev.novaclient.module.hud.BlockInfoModule;
import dev.novaclient.module.hud.InventoryPreviewModule;
import dev.novaclient.module.hud.OnlinePlayersModule;
import dev.novaclient.module.hud.StopwatchModule;
import dev.novaclient.module.hud.WaypointModule;
import dev.novaclient.module.hud.GameTimeModule;
import dev.novaclient.module.hud.MoonPhaseModule;
import dev.novaclient.module.hud.RotationModule;
import dev.novaclient.module.hud.ExperienceModule;
import dev.novaclient.module.hud.HeldDurabilityModule;
import dev.novaclient.module.utility.AutoGgModule;
import dev.novaclient.module.utility.WindowTitleModule;
import dev.novaclient.module.visual.HandSizeModule;
import dev.novaclient.module.visual.DamageNumbersModule;
import dev.novaclient.module.visual.HealthTagsModule;
import dev.novaclient.module.visual.RearViewModule;
import dev.novaclient.module.utility.ChatMentionsModule;
import dev.novaclient.module.hud.MinimapModule;
import dev.novaclient.module.hud.MovementStatusModule;
import dev.novaclient.module.hud.HotbarNumbersModule;
import dev.novaclient.module.cosmetics.KillEffectModule;
import dev.novaclient.module.hud.WeatherModule;
import dev.novaclient.module.utility.BackgroundFpsModule;
import dev.novaclient.module.utility.ChatMacrosModule;
import dev.novaclient.module.cosmetics.HitParticlesModule;
import dev.novaclient.module.cosmetics.NametagModule;
import dev.novaclient.module.cosmetics.TrailModule;
import dev.novaclient.module.hud.CompassModule;
import dev.novaclient.module.hud.DateModule;
import dev.novaclient.module.hud.HudModule.Anchor;
import dev.novaclient.module.hud.ItemCounterModule;
import dev.novaclient.module.hud.LightLevelModule;
import dev.novaclient.module.hud.SaturationModule;
import dev.novaclient.module.hud.TargetHudModule;
import dev.novaclient.module.utility.ChatTimestampsModule;
import dev.novaclient.module.utility.DeathCoordsModule;
import dev.novaclient.module.utility.DurabilityWarningModule;
import dev.novaclient.module.utility.NotificationsModule;
import dev.novaclient.module.visual.BlockOutlineModule;
import dev.novaclient.module.visual.OverlaysModule;
import dev.novaclient.module.utility.ToggleSneakModule;
import dev.novaclient.module.utility.ToggleSprintModule;
import dev.novaclient.module.visual.CrosshairModule;
import dev.novaclient.module.visual.FreelookModule;
import dev.novaclient.module.visual.FullbrightModule;
import dev.novaclient.module.visual.HudTweaksModule;
import dev.novaclient.module.visual.NoWeatherModule;
import dev.novaclient.module.visual.LowFireModule;
import dev.novaclient.module.visual.NoHurtCamModule;
import dev.novaclient.module.visual.ZoomModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class ModuleManager {
	private final List<Module> modules = new ArrayList<>();

	public final ThemeModule theme;
	public final ZoomModule zoom;
	public final FreelookModule freelook;
	public final CrosshairModule crosshair;
	public final PotionEffectsModule potionEffects;

	public ModuleManager() {
		theme = register(new ThemeModule());
		register(new FpsModule());
		register(new PingModule());
		register(new CpsModule());
		register(new CoordinatesModule());
		register(new KeystrokesModule());
		register(new ArmorStatusModule());
		potionEffects = register(new PotionEffectsModule());
		register(new ClockModule());
		register(new ServerAddressModule());
		register(new SpeedModule());
		register(new MemoryModule());
		register(new DayCounterModule());
		register(new BiomeModule());
		register(new ArrowCounterModule());
		register(new SessionTimeModule());
		register(new ReachModule());
		register(new ComboModule());
		register(new TargetHudModule());
		register(new CompassModule());
		register(new LightLevelModule());
		register(new SaturationModule());
		register(new DateModule());
		register(new ItemCounterModule("Totem-Zähler", "Wie viele Totems du dabei hast", "Totems", Anchor.END, 2, Anchor.START, 160));
		register(new BlockInfoModule());
		register(new InventoryPreviewModule());
		register(new OnlinePlayersModule());
		register(new WeatherModule());
		register(new StopwatchModule());
		register(new WaypointModule());
		register(new GameTimeModule());
		register(new MoonPhaseModule());
		register(new RotationModule());
		register(new ExperienceModule());
		register(new HeldDurabilityModule());
		register(new MinimapModule());
		register(new MovementStatusModule());
		register(new HotbarNumbersModule());
		register(new MusicModule());
		register(new ItemCounterModule("Item-Zähler", "Zählt das gehaltene Item im ganzen Inventar", "Gehaltenes Item", Anchor.START, 2, Anchor.START, 196));

		zoom = register(new ZoomModule());
		freelook = register(new FreelookModule());
		crosshair = register(new CrosshairModule());
		register(new FullbrightModule());
		register(new NoHurtCamModule());
		register(new LowFireModule());
		register(new HudTweaksModule());
		register(new NoWeatherModule());
		register(new BlockOutlineModule());
		register(new OverlaysModule());
		register(new HandSizeModule());
		register(new DamageNumbersModule());
		register(new HealthTagsModule());
		register(new RearViewModule());
		register(new dev.novaclient.module.visual.ItemPhysicsModule());
		register(new dev.novaclient.module.visual.ScoreboardNumbersModule());
		register(new dev.novaclient.module.visual.HitMarkerModule());

		register(new ToggleSprintModule());
		register(new ToggleSneakModule());
		register(new NotificationsModule());
		register(new DeathCoordsModule());
		register(new DurabilityWarningModule());
		register(new ChatTimestampsModule());
		register(new ChatMacrosModule());
		register(new BackgroundFpsModule());
		register(new NetworkModule());
		register(new TabPingModule());
		register(new CoordsShareModule());
		register(new AutoGgModule());
		register(new WindowTitleModule());
		register(new ChatMentionsModule());
		register(new dev.novaclient.module.utility.CompactChatModule());
		register(new dev.novaclient.module.utility.LongChatModule());
		register(new dev.novaclient.module.utility.FriendsModule());

		register(new CapeModule());
		register(new HeadCosmeticModule());
		register(new WingsModule());
		register(new EmotesModule());
		register(new NametagModule());
		register(new TrailModule());
		register(new HitParticlesModule());
		register(new CompanionModule());
		register(new AuraModule());
		register(new MinimeeModule());
		register(new ExtrasModule());
		register(new KillEffectModule());
	}

	private <M extends Module> M register(M module) {
		modules.add(module);
		return module;
	}

	public List<Module> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public List<Module> getModules(Category category) {
		List<Module> out = new ArrayList<>();
		for (Module m : modules) {
			if (m.getCategory() == category) {
				out.add(m);
			}
		}
		return out;
	}

	public List<HudModule> getHudModules() {
		List<HudModule> out = new ArrayList<>();
		for (Module m : modules) {
			if (m instanceof HudModule h) {
				out.add(h);
			}
		}
		return out;
	}

	public Module byName(String name) {
		for (Module m : modules) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	public void tick(Minecraft mc) {
		CombatTracker.tick(mc);
		for (Module m : modules) {
			m.tick();
		}
	}

	/** Called for every new key press while in game (no screen open). */
	public void onKeyPressed(int key) {
		if (key <= 0) {
			return;
		}
		for (Module m : modules) {
			if (m.getKeybind() == key) {
				m.toggle();
				Notifications.push(m.getName() + (m.isEnabled() ? " aktiviert" : " deaktiviert"), m.isEnabled() ? 0 : 0xFF777788);
			}
		}
	}
}
