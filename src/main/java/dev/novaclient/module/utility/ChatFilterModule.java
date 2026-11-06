package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.TextSetting;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Hides chat messages that contain one of your words (e.g. spam, ads, words you do not want to read). */
public class ChatFilterModule extends Module {
	private static ChatFilterModule instance;
	public final TextSetting words = add(new TextSetting("Wörter (mit Komma getrennt)", "", 200));
	public final BoolSetting caseSensitive = add(new BoolSetting("Groß-/Kleinschreibung beachten", false));
	private int hidden;

	public ChatFilterModule() {
		super("Chat-Filter", "Blendet Chat-Nachrichten aus, die eines deiner Wörter enthalten", Category.UTILITY, false);
		instance = this;
	}

	public static ChatFilterModule get() {
		return instance;
	}

	/** True if the message should not be shown. */
	public static boolean hides(Component message) {
		ChatFilterModule m = instance;
		if (m == null || !m.isEnabled() || message == null || m.words.get().isBlank()) {
			return false;
		}
		boolean cs = m.caseSensitive.isOn();
		String text = cs ? message.getString() : message.getString().toLowerCase(Locale.ROOT);
		for (String w : m.words.get().split(",")) {
			String word = cs ? w.strip() : w.strip().toLowerCase(Locale.ROOT);
			if (!word.isEmpty() && text.contains(word)) {
				m.hidden++;
				return true;
			}
		}
		return false;
	}

	/** Number of hidden messages since the start (tests). */
	public int hidden() {
		return hidden;
	}
}
