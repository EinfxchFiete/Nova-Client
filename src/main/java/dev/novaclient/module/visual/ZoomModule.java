package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.setting.KeySetting;
import net.minecraft.client.Minecraft;

/** OptiFine-style zoom: hold the zoom key (default C), scroll to change the zoom level. */
public class ZoomModule extends Module {
	private static ZoomModule instance;

	public final KeySetting zoomKey = add(new KeySetting("Zoom-Taste", InputConstants.KEY_C));
	public final NumberSetting zoomFactor = add(new NumberSetting("Zoom-Stärke", 4, 1.5, 50, 0.5));
	public final BoolSetting scrollToZoom = add(new BoolSetting("Mit Mausrad zoomen", true));
	public final BoolSetting smooth = add(new BoolSetting("Weicher Zoom", true));
	public final BoolSetting cinematic = add(new BoolSetting("Filmische Kamera", true));

	private boolean zooming;
	private double scrollFactor;
	private boolean changedSmoothCamera;
	private boolean previousSmoothCamera;
	private float currentMultiplier = 1.0F;
	private long lastFrame;

	public ZoomModule() {
		super("Zoom", "Halte C zum Heranzoomen, Mausrad ändert die Stärke", Category.VISUAL, true);
		instance = this;
	}

	public static boolean isZooming() {
		return instance != null && instance.zooming;
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		boolean shouldZoom = isEnabled() && zoomKey.isDown() && mc.player != null && mc.gui.screen() == null;
		if (shouldZoom && !zooming) {
			scrollFactor = zoomFactor.get();
			if (cinematic.isOn()) {
				previousSmoothCamera = mc.options.smoothCamera;
				mc.options.smoothCamera = true;
				changedSmoothCamera = true;
			}
		} else if (!shouldZoom && zooming) {
			restoreCamera();
		}
		zooming = shouldZoom;
	}

	private void restoreCamera() {
		if (changedSmoothCamera) {
			mc().options.smoothCamera = previousSmoothCamera;
			changedSmoothCamera = false;
		}
	}

	@Override
	protected void onDisable() {
		zooming = false;
		restoreCamera();
	}

	/** @return true if the scroll event was consumed by the zoom. */
	public static boolean onScroll(double amount) {
		ZoomModule z = instance;
		if (z == null || !z.zooming || !z.scrollToZoom.isOn() || amount == 0) {
			return false;
		}
		double f = amount > 0 ? z.scrollFactor * 1.2 : z.scrollFactor / 1.2;
		z.scrollFactor = Math.max(1.0, Math.min(z.zoomFactor.getMax(), f));
		return true;
	}

	/** Multiplier applied to the vanilla field of view (1 = no zoom). Called every frame. */
	public static float fovMultiplier() {
		ZoomModule z = instance;
		if (z == null) {
			return 1.0F;
		}
		float target = z.zooming ? (float) (1.0 / Math.max(1.0, z.scrollFactor)) : 1.0F;
		long now = System.nanoTime();
		float dt = z.lastFrame == 0 ? 1.0F : Math.min(1.0F, (now - z.lastFrame) / 1.0E9F);
		z.lastFrame = now;
		if (!z.smooth.isOn()) {
			z.currentMultiplier = target;
		} else {
			float k = 1.0F - (float) Math.exp(-dt * 14.0F);
			z.currentMultiplier += (target - z.currentMultiplier) * k;
			if (Math.abs(target - z.currentMultiplier) < 0.001F) {
				z.currentMultiplier = target;
			}
		}
		return z.currentMultiplier;
	}
}
