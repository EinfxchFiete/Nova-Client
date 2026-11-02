package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/** Rotating particle ring: around you (aura) or above your head (halo). */
public class AuraModule extends Module {
	public final ModeSetting particle = add(new ModeSetting("Partikel", "Endstab", "Endstab", "Seelenflammen", "Flammen", "Glitzer", "Funken", "Noten"));
	private final boolean halo;
	private float angle;

	public AuraModule(boolean halo) {
		super(halo ? "Heiligenschein" : "Aura",
			halo ? "Ein leuchtender Ring über deinem Kopf" : "Ein Partikelring, der um dich kreist",
			Category.COSMETICS, false);
		this.halo = halo;
	}

	private SimpleParticleType type() {
		return switch (particle.get()) {
			case "Seelenflammen" -> ParticleTypes.SOUL_FIRE_FLAME;
			case "Flammen" -> ParticleTypes.FLAME;
			case "Glitzer" -> ParticleTypes.HAPPY_VILLAGER;
			case "Funken" -> ParticleTypes.ENCHANTED_HIT;
			case "Noten" -> ParticleTypes.NOTE;
			default -> ParticleTypes.END_ROD;
		};
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		if (!isEnabled() || p == null || mc.level == null || p.isSpectator() || p.isInvisible()) {
			return;
		}
		angle += halo ? 0.5F : 0.35F;
		int points = halo ? 2 : 2;
		for (int i = 0; i < points; i++) {
			double a = angle + i * Math.PI;
			double r = halo ? 0.32 : 0.9;
			double y = halo ? p.getY() + p.getBbHeight() + 0.25 : p.getY() + 0.9 + Math.sin(angle * 0.7 + i) * 0.5;
			mc.level.addParticle(type(), p.getX() + Math.cos(a) * r, y, p.getZ() + Math.sin(a) * r, 0, 0, 0);
		}
	}
}
