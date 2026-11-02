package dev.novaclient.module.hud;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class BiomeModule extends TextHudModule {
	public BiomeModule() {
		super("Biom", "Das Biom, in dem du stehst", false, Anchor.END, 2, Anchor.START, 82);
	}

	@Override
	protected String label() {
		return "Biom";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		if (p == null || mc().level == null) {
			return "Ebene";
		}
		return mc().level.getBiome(p.blockPosition()).unwrapKey()
			.map(key -> Component.translatable(key.identifier().toLanguageKey("biome")).getString())
			.orElse("?");
	}
}
