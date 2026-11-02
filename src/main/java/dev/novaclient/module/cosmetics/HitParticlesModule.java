package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;

/** More hit particles when you attack (client-side). */
public class HitParticlesModule extends Module {
	private static HitParticlesModule instance;

	public final ModeSetting type = add(new ModeSetting("Art", "Kritisch", "Kritisch", "Magie", "Beides"));
	public final NumberSetting multiplier = add(new NumberSetting("Multiplikator", 2, 1, 5, 1));

	public HitParticlesModule() {
		super("Treffer-Partikel", "Mehr Partikel bei jedem Treffer", Category.COSMETICS, false);
		instance = this;
	}

	public static void onHit(Entity target) {
		HitParticlesModule h = instance;
		if (h == null || !h.isEnabled()) {
			return;
		}
		for (int i = 0; i < h.multiplier.getInt(); i++) {
			if (!h.type.is("Magie")) {
				mc().particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
			}
			if (!h.type.is("Kritisch")) {
				mc().particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
			}
		}
	}
}
