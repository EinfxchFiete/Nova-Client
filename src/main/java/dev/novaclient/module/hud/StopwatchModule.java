package dev.novaclient.module.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.setting.KeySetting;

/** A stopwatch: one key starts/stops, another resets. */
public class StopwatchModule extends TextHudModule {
	public final KeySetting startStop = add(new KeySetting("Start/Stopp-Taste", InputConstants.KEY_NUMPAD0));
	public final KeySetting resetKey = add(new KeySetting("Zurücksetzen-Taste", InputConstants.KEY_NUMPAD1));

	private long startedAt;
	private long accumulated;
	private boolean running;

	public StopwatchModule() {
		super("Stoppuhr", "Stoppuhr mit Tasten (Standard: Num 0 = Start/Stopp, Num 1 = Reset)", false, Anchor.CENTER, 0, Anchor.START, 62);
	}

	public boolean isRunning() {
		return running;
	}

	public long elapsedMillis() {
		return accumulated + (running ? System.currentTimeMillis() - startedAt : 0);
	}

	@Override
	public void tick() {
		boolean inGame = mc().gui.screen() == null;
		if (startStop.consumePress(inGame) && isEnabled()) {
			if (running) {
				accumulated += System.currentTimeMillis() - startedAt;
			} else {
				startedAt = System.currentTimeMillis();
			}
			running = !running;
		}
		if (resetKey.consumePress(inGame) && isEnabled()) {
			running = false;
			accumulated = 0;
		}
	}

	@Override
	protected String label() {
		return running ? "Stoppuhr ▶" : "Stoppuhr";
	}

	@Override
	protected String value() {
		long ms = elapsedMillis();
		return String.format("%d:%02d.%d", ms / 60000, (ms / 1000) % 60, (ms / 100) % 10);
	}
}
