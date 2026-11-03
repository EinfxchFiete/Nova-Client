package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;

/** Hides the red score numbers on the right of the scoreboard sidebar. */
public class ScoreboardNumbersModule extends Module {
	private static ScoreboardNumbersModule instance;

	public ScoreboardNumbersModule() {
		super("Scoreboard-Zahlen aus", "Blendet die roten Zahlen rechts im Scoreboard aus", Category.VISUAL, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
