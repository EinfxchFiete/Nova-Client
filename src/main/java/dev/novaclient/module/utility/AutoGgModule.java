package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.setting.TextSetting;
import java.util.Locale;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/**
 * Writes "gg" when a round ends. Only server (system) messages are checked, never messages of other players,
 * and at most once every 15 seconds.
 */
public class AutoGgModule extends Module {
	private static final long COOLDOWN_MS = 15_000;

	public final TextSetting message = add(new TextSetting("Nachricht", "gg", 60));
	public final TextSetting triggers = add(new TextSetting("Auslöser (mit Komma)",
		"you won,victory,winner:,1st killer,gewonnen,hat gewonnen,game over,spiel vorbei", 200));
	public final NumberSetting delay = add(new NumberSetting("Verzögerung (s)", 1, 0, 5, 0.5));

	private long lastSent;
	private long sendAt;

	public AutoGgModule() {
		super("Auto-GG", "Schreibt automatisch „gg“, wenn eine Runde vorbei ist", Category.UTILITY, false);
		ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
	}

	private void onGameMessage(Component text, boolean overlay) {
		Minecraft mc = Minecraft.getInstance();
		if (!isEnabled() || mc.player == null || mc.isLocalServer() || sendAt != 0) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastSent < COOLDOWN_MS) {
			return;
		}
		String msg = text.getString().toLowerCase(Locale.ROOT);
		for (String t : triggers.get().split(",")) {
			String trigger = t.trim().toLowerCase(Locale.ROOT);
			if (trigger.length() >= 3 && msg.contains(trigger)) {
				sendAt = now + Math.round(delay.get() * 1000);
				return;
			}
		}
	}

	@Override
	public void tick() {
		if (sendAt == 0 || System.currentTimeMillis() < sendAt) {
			return;
		}
		sendAt = 0;
		ClientPacketListener connection = mc().getConnection();
		String text = message.get().trim();
		if (connection == null || text.isEmpty() || text.startsWith("/")) {
			return;
		}
		lastSent = System.currentTimeMillis();
		connection.sendChat(text);
	}
}
