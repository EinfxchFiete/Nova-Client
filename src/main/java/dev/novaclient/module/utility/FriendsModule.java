package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;

/** Settings of the friend system (the friend list itself is in the friends menu). */
public class FriendsModule extends Module {
	private static FriendsModule instance;

	public final BoolSetting shareServer = add(new BoolSetting("Server mit Freunden teilen", true));
	public final BoolSetting mark = add(new BoolSetting("Freunde markieren (Name und Tab-Liste)", true));
	public final BoolSetting notify = add(new BoolSetting("Benachrichtigungen (online, Anfragen)", true));

	public FriendsModule() {
		super("Freunde", "Freundesliste: sieh, wann und wo deine Freunde spielen (über das Nova-Netzwerk)", Category.UTILITY, true);
		instance = this;
	}

	public static FriendsModule get() {
		return instance;
	}

	public static boolean marking() {
		return instance != null && instance.isEnabled() && instance.mark.isOn();
	}
}
