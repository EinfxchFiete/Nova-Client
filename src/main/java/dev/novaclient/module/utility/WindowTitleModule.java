package dev.novaclient.module.utility;

import dev.novaclient.NovaClient;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.world.WorldId;
import net.minecraft.client.Minecraft;

/** Window title "Nova Client 1.5.0 · <server>" instead of "Minecraft* 26.3 - Multiplayer". */
public class WindowTitleModule extends Module {
	private static WindowTitleModule instance;

	public final BoolSetting showServer = add(new BoolSetting("Server/Welt im Titel", true));

	public WindowTitleModule() {
		super("Fenstertitel", "Zeigt „Nova Client“ und den Server im Fenstertitel", Category.UTILITY, true);
		instance = this;
	}

	/** The title to use, or null to keep Minecraft's. */
	public static String title(Minecraft mc) {
		if (instance == null || !instance.isEnabled()) {
			return null;
		}
		String t = NovaClient.NAME + " " + NovaClient.VERSION + " · Minecraft 26.3";
		if (instance.showServer.isOn() && mc.level != null) {
			t += " · " + WorldId.displayName(mc);
		}
		return t;
	}

	@Override
	protected void onEnable() {
		mc().updateTitle();
	}

	@Override
	protected void onDisable() {
		mc().updateTitle();
	}

	@Override
	public void tick() {
		// the world/server changes without Minecraft always updating the title
		String wanted = title(mc());
		if (wanted != null && !wanted.equals(last)) {
			last = wanted;
			mc().updateTitle();
		}
	}

	private String last;
}
