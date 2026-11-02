package dev.novaclient.module;

import dev.novaclient.module.hud.ArmorStatusModule;
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
import dev.novaclient.module.utility.ToggleSneakModule;
import dev.novaclient.module.utility.ToggleSprintModule;
import dev.novaclient.module.visual.FullbrightModule;
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

		zoom = register(new ZoomModule());
		register(new FullbrightModule());
		register(new NoHurtCamModule());
		register(new LowFireModule());

		register(new ToggleSprintModule());
		register(new ToggleSneakModule());
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
			}
		}
	}
}
