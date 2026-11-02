package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.network.chat.Component;

/** Adds the time in front of every chat message. */
public class ChatTimestampsModule extends Module {
	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static ChatTimestampsModule instance;

	public final BoolSetting seconds = add(new BoolSetting("Sekunden", false));

	public ChatTimestampsModule() {
		super("Chat-Zeitstempel", "Uhrzeit vor jeder Chat-Nachricht", Category.UTILITY, false);
		instance = this;
	}

	public static Component apply(Component message) {
		ChatTimestampsModule c = instance;
		if (c == null || !c.isEnabled() || message == null) {
			return message;
		}
		String time = LocalTime.now().format(c.seconds.isOn() ? HMS : HM);
		return Component.literal("[" + time + "] ").withColor(0x9A9AAA).append(message);
	}
}
