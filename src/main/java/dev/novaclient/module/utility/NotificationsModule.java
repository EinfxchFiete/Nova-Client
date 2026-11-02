package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.util.Notifications;

/** Small pop-ups when you switch a module with its key, plus warnings. */
public class NotificationsModule extends Module {
	public NotificationsModule() {
		super("Benachrichtigungen", "Kurze Hinweise, wenn du Module per Taste schaltest", Category.UTILITY, true);
	}

	@Override
	public void tick() {
		Notifications.setEnabled(isEnabled());
	}
}
