package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.KeySetting;
import dev.novaclient.setting.TextSetting;
import dev.novaclient.util.Notifications;
import net.minecraft.client.player.LocalPlayer;

/** Sends your coordinates to the chat with one key (e.g. for your team). */
public class CoordsShareModule extends Module {
	public final KeySetting key = add(new KeySetting("Taste", 0));
	public final TextSetting prefix = add(new TextSetting("Text davor", "Ich bin bei", 40));

	public CoordsShareModule() {
		super("Koordinaten teilen", "Eine Taste schreibt deine Position in den Chat", Category.UTILITY, false);
	}

	@Override
	public void tick() {
		LocalPlayer p = mc().player;
		if (key.consumePress(mc().gui.screen() == null && p != null) && isEnabled() && p != null && mc().getConnection() != null) {
			String msg = (prefix.get().isBlank() ? "" : prefix.get().trim() + " ")
				+ "X " + p.blockPosition().getX() + ", Y " + p.blockPosition().getY() + ", Z " + p.blockPosition().getZ();
			mc().getConnection().sendChat(msg);
			Notifications.push("Koordinaten gesendet", 0);
		}
	}
}
