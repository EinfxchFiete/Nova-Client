package dev.novaclient.module.visual;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.KeySetting;
import dev.novaclient.setting.ModeSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Look around without turning your player (default key: Left Alt, changeable in the menu). */
public class FreelookModule extends Module {
	private static FreelookModule instance;

	public final KeySetting key = add(new KeySetting("Freelook-Taste", InputConstants.KEY_LALT));
	public final ModeSetting mode = add(new ModeSetting("Modus", "Halten", "Halten", "Umschalten"));
	public final BoolSetting thirdPerson = add(new BoolSetting("Dritte Person", true));
	public final BoolSetting invertPitch = add(new BoolSetting("Y-Achse umkehren", false));
	public final BoolSetting indicator = add(new BoolSetting("Anzeige auf dem Bildschirm", true));

	private boolean active;
	private boolean toggled;
	private boolean wasDown;
	private float yaw;
	private float pitch;
	private CameraType previousCamera;

	public FreelookModule() {
		super("Freelook", "Umsehen ohne dich zu drehen: Linke Alt-Taste halten", Category.VISUAL, true);
		instance = this;
	}

	public static boolean isActive() {
		return instance != null && instance.active;
	}

	public static boolean showIndicator() {
		return isActive() && instance.indicator.isOn();
	}

	public static float yaw() {
		return instance.yaw;
	}

	public static float pitch() {
		return instance.pitch;
	}

	/** Mouse movement while freelooking (same scale as Entity#turn). */
	public static void turn(double xo, double yo) {
		FreelookModule f = instance;
		float dy = (float) yo * 0.15F;
		if (f.invertPitch.isOn()) {
			dy = -dy;
		}
		f.pitch = Mth.clamp(f.pitch + dy, -90.0F, 90.0F);
		f.yaw += (float) xo * 0.15F;
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		boolean inGame = mc.player != null && mc.gui.screen() == null;
		boolean down = inGame && key.isDown();
		if (down && !wasDown && mode.is("Umschalten")) {
			toggled = !toggled;
		}
		wasDown = down;
		if (!mode.is("Umschalten")) {
			toggled = false;
		}
		boolean pressed = mode.is("Halten") ? down : toggled;
		boolean shouldBeActive = isEnabled() && pressed && mc.player != null && (mc.gui.screen() == null || active && mode.is("Umschalten"));
		if (shouldBeActive && !active) {
			start(mc);
		} else if (!shouldBeActive && active) {
			stop(mc);
		}
	}

	private void start(Minecraft mc) {
		active = true;
		yaw = mc.player.getYRot();
		pitch = mc.player.getXRot();
		previousCamera = mc.options.getCameraType();
		if (thirdPerson.isOn() && previousCamera.isFirstPerson()) {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
	}

	private void stop(Minecraft mc) {
		active = false;
		toggled = false;
		if (previousCamera != null) {
			mc.options.setCameraType(previousCamera);
			previousCamera = null;
		}
	}

	@Override
	protected void onDisable() {
		if (active) {
			stop(mc());
		}
	}
}
