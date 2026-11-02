package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Lowers the FPS limit while Minecraft is in the background (saves power and heat). */
public class BackgroundFpsModule extends Module {
	public final NumberSetting limit = add(new NumberSetting("FPS im Hintergrund", 15, 10, 60, 5));

	private Integer savedLimit;

	public BackgroundFpsModule() {
		super("Hintergrund-FPS", "Weniger FPS, wenn das Fenster nicht im Vordergrund ist", Category.UTILITY, false);
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		boolean background = isEnabled() && !mc.isWindowActive();
		if (background && savedLimit == null) {
			savedLimit = mc.options.framerateLimit().get();
			mc.options.framerateLimit().set(limit.getInt());
		} else if (!background && savedLimit != null) {
			restore();
		}
	}

	public void restore() {
		if (savedLimit != null) {
			mc().options.framerateLimit().set(savedLimit);
			savedLimit = null;
		}
	}

	@Override
	protected void onDisable() {
		restore();
	}
}
