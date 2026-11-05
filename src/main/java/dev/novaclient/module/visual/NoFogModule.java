package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Removes the fog at the end of the render distance and (optionally) the weather/biome fog. Water and lava fog stay. */
public class NoFogModule extends Module {
	private static NoFogModule instance;
	public final BoolSetting distance = add(new BoolSetting("Sichtweiten-Nebel entfernen", true));
	public final BoolSetting environment = add(new BoolSetting("Wetter- und Biom-Nebel entfernen", true));

	public NoFogModule() {
		super("Kein Nebel", "Kein Nebel am Ende der Sichtweite und bei Regen/Nether (Wasser- und Lava-Nebel bleiben)", Category.VISUAL, false);
		instance = this;
	}

	/** Changes the fog of this frame; {@code atmospheric} = not in water, lava or powder snow. */
	public static void apply(FogData fog, Entity camera, boolean atmospheric) {
		NoFogModule m = instance;
		if (m == null || !m.isEnabled() || !atmospheric) {
			return;
		}
		// blindness and darkness are game mechanics, they stay
		if (camera instanceof LivingEntity le && (le.hasEffect(MobEffects.BLINDNESS) || le.hasEffect(MobEffects.DARKNESS))) {
			return;
		}
		if (m.distance.isOn()) {
			fog.renderDistanceStart = 1.0E7F;
			fog.renderDistanceEnd = 1.0E7F;
		}
		if (m.environment.isOn()) {
			fog.environmentalStart = 1.0E7F;
			fog.environmentalEnd = 1.0E7F;
		}
	}
}
