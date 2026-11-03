package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.TextSetting;
import dev.novaclient.util.Notifications;
import java.util.Locale;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Plays a sound and shows a notification when someone writes your name (or another word) in the chat. */
public class ChatMentionsModule extends Module {
	public final BoolSetting sound = add(new BoolSetting("Ton abspielen", true));
	public final TextSetting extra = add(new TextSetting("Weitere Wörter (mit Komma)", "", 100));

	private long lastPing;

	public ChatMentionsModule() {
		super("Chat-Erwähnungen", "Ton und Hinweis, wenn jemand deinen Namen im Chat schreibt", Category.UTILITY, false);
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && sender != null && sender.id().equals(mc.player.getUUID())) {
				return;
			}
			check(message, sender == null ? null : sender.name());
		});
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				check(message, null);
			}
		});
	}

	/** True if the text mentions you (tests). */
	public boolean mentions(String text) {
		Minecraft mc = Minecraft.getInstance();
		String t = text.toLowerCase(Locale.ROOT);
		if (mc.player != null) {
			String name = mc.player.getGameProfile().name().toLowerCase(Locale.ROOT);
			if (!name.isEmpty() && t.matches("(?s).*(^|[^a-z0-9_])" + java.util.regex.Pattern.quote(name) + "([^a-z0-9_]|$).*")) {
				return true;
			}
		}
		for (String w : extra.get().split(",")) {
			String word = w.trim().toLowerCase(Locale.ROOT);
			if (word.length() >= 2 && t.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private void check(Component message, String sender) {
		Minecraft mc = Minecraft.getInstance();
		if (!isEnabled() || mc.player == null) {
			return;
		}
		String text = message.getString();
		// don't react to our own chat line ("<Name> ...") that the server echoes as a game message
		if (text.startsWith("<" + mc.player.getGameProfile().name() + ">")) {
			return;
		}
		if (!mentions(text)) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastPing < 1500) {
			return;
		}
		lastPing = now;
		mc.execute(() -> {
			if (sound.isOn() && mc.player != null) {
				mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.4F);
			}
			Notifications.push(sender != null ? "Erwähnt von " + sender : "Du wurdest im Chat erwähnt", 0);
		});
	}
}
