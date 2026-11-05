package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.util.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

/** Reminds you to take a break after playing for a while (counts only while the game is not paused). */
public class BreakReminderModule extends Module {
	public final NumberSetting minutes = add(new NumberSetting("Alle (Minuten)", 60, 15, 180, 5));
	private long playedMs;
	private long lastTick;
	private int reminders;

	public BreakReminderModule() {
		super("Pausen-Erinnerung", "Erinnert dich nach einer Weile an eine kurze Pause (Augen, Trinken, Bewegen)", Category.UTILITY, false);
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		long dt = lastTick == 0 ? 0 : Math.min(1000, now - lastTick);
		lastTick = now;
		if (!isEnabled() || mc.player == null || mc.isPaused()) {
			return;
		}
		playedMs += dt;
		long every = Math.round(minutes.get() * 60_000);
		if (playedMs >= every) {
			playedMs = 0;
			reminders++;
			long total = reminders * every / 60_000;
			Notifications.push("Zeit für eine Pause! Du spielst seit " + (total >= 60 ? total / 60 + " h " + (total % 60 > 0 ? total % 60 + " min" : "") : total + " min").trim(),
				0xFF4FC3F7);
			mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 1.2F);
		}
	}

	@Override
	protected void onEnable() {
		playedMs = 0;
		reminders = 0;
	}

	/** Played time since the last reminder in ms (tests). */
	public long playedMs() {
		return playedMs;
	}

	/** For tests: pretend this much time was played. */
	public void addPlayedForTest(long ms) {
		playedMs += ms;
	}
}
