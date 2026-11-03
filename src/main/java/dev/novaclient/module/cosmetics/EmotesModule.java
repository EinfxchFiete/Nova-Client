package dev.novaclient.module.cosmetics;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.emote.EmoteWheelScreen;
import dev.novaclient.emote.Emotes;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.KeySetting;

/** Emote wheel (default key V; G is "quick actions" in 26.3). Other Nova players see your emotes. */
public class EmotesModule extends Module {
	private static EmotesModule instance;

	public final KeySetting wheelKey = add(new KeySetting("Emote-Rad-Taste", InputConstants.KEY_V));
	public final BoolSetting switchCamera = add(new BoolSetting("Kamera nach vorne drehen", true));

	public EmotesModule() {
		super("Emotes", "Winken, Tanzen, Jubeln … per Emote-Rad (Taste V)", Category.COSMETICS, true);
		instance = this;
	}

	public static EmotesModule get() {
		return instance;
	}

	@Override
	public void tick() {
		boolean inGame = mc().gui.screen() == null && mc().player != null;
		if (wheelKey.consumePress(inGame) && isEnabled()) {
			mc().gui.setScreen(new EmoteWheelScreen());
		}
		Emotes.tick(mc());
	}

	@Override
	protected void onDisable() {
		if (Emotes.localActive() != null) {
			Emotes.stop();
		}
	}
}
