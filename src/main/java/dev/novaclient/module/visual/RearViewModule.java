package dev.novaclient.module.visual;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.KeySetting;
import dev.novaclient.util.KeyState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Hold a key to look behind you (third person front), release to go back. */
public class RearViewModule extends Module {
	public final KeySetting key = add(new KeySetting("Rückblick-Taste", InputConstants.KEY_H));
	private CameraType previous;

	public RearViewModule() {
		super("Rückblick", "Taste halten, um hinter dich zu schauen", Category.VISUAL, false);
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		boolean held = isEnabled() && mc.gui.screen() == null && key.code() != 0 && KeyState.isDown(key.code());
		if (held && previous == null) {
			previous = mc.options.getCameraType();
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		} else if (!held && previous != null) {
			mc.options.setCameraType(previous);
			previous = null;
		}
	}

	@Override
	protected void onDisable() {
		if (previous != null) {
			mc().options.setCameraType(previous);
			previous = null;
		}
	}
}
