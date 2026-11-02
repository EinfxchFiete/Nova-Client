package dev.novaclient.module.hud;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.LightLayer;

public class LightLevelModule extends TextHudModule {
	public LightLevelModule() {
		super("Lichtlevel", "Blocklicht an deinen Füßen (unter 1 können Monster spawnen)", false, Anchor.END, 2, Anchor.START, 186);
	}

	@Override
	protected String label() {
		return "Licht";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		if (p == null || mc().level == null) {
			return "15";
		}
		int block = mc().level.getBrightness(LightLayer.BLOCK, p.blockPosition());
		int sky = mc().level.getBrightness(LightLayer.SKY, p.blockPosition());
		return block + " (Himmel " + sky + ")";
	}
}
