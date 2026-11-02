package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;

/** Hides vanilla HUD parts. */
public class HudTweaksModule extends Module {
	private static HudTweaksModule instance;

	public final BoolSetting hideScoreboard = add(new BoolSetting("Scoreboard ausblenden", true));
	public final BoolSetting hideBossBar = add(new BoolSetting("Bossleiste ausblenden", false));
	public final BoolSetting hideTitles = add(new BoolSetting("Titel ausblenden", false));

	public HudTweaksModule() {
		super("HUD-Tweaks", "Blendet Scoreboard, Bossleiste oder Titel aus", Category.VISUAL, false);
		instance = this;
	}

	public static boolean hideScoreboard() {
		return instance != null && instance.isEnabled() && instance.hideScoreboard.isOn();
	}

	public static boolean hideBossBar() {
		return instance != null && instance.isEnabled() && instance.hideBossBar.isOn();
	}

	public static boolean hideTitles() {
		return instance != null && instance.isEnabled() && instance.hideTitles.isOn();
	}
}
