package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.KeySetting;
import dev.novaclient.setting.TextSetting;
import dev.novaclient.util.Notifications;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Up to three keys that send a chat message or command, e.g. "/spawn". */
public class ChatMacrosModule extends Module {
	private final KeySetting[] keys = new KeySetting[3];
	private final TextSetting[] texts = new TextSetting[3];
	private String lastSent = "";

	public ChatMacrosModule() {
		super("Chat-Makros", "Tasten, die eine Nachricht oder einen Befehl senden", Category.UTILITY, false);
		String[] defaults = {"/spawn", "gg", ""};
		for (int i = 0; i < 3; i++) {
			keys[i] = add(new KeySetting("Makro " + (i + 1) + " Taste", 0));
			texts[i] = add(new TextSetting("Makro " + (i + 1) + " Text", defaults[i], 100));
		}
	}

	public KeySetting key(int i) {
		return keys[i];
	}

	public TextSetting text(int i) {
		return texts[i];
	}

	public String getLastSent() {
		return lastSent;
	}

	@Override
	public void tick() {
		boolean inGame = mc().gui.screen() == null && mc().player != null;
		for (int i = 0; i < 3; i++) {
			if (keys[i].consumePress(inGame) && isEnabled() && keys[i].code() > 0) {
				send(texts[i].get().trim());
			}
		}
	}

	private void send(String text) {
		ClientPacketListener connection = mc().getConnection();
		if (text.isEmpty() || connection == null) {
			return;
		}
		if (text.startsWith("/")) {
			connection.sendCommand(text.substring(1));
		} else {
			connection.sendChat(text);
		}
		lastSent = text;
		Notifications.push("Gesendet: " + text, 0);
	}
}
