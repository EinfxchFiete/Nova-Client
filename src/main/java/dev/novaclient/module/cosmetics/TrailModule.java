package dev.novaclient.module.cosmetics;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import dev.novaclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Client-side particle trail behind your player. */
public class TrailModule extends Module {
	public final ModeSetting type = add(new ModeSetting("Partikel", "Herzen",
		"Herzen", "Flammen", "Seelenflammen", "Noten", "Endstab", "Kirschblüten", "Glitzer", "Totem", "Funken"));
	public final NumberSetting amount = add(new NumberSetting("Menge", 2, 1, 6, 1));
	public final BoolSetting onlyMoving = add(new BoolSetting("Nur beim Laufen", true));

	private final RandomSource random = RandomSource.create();
	private int counter;

	public TrailModule() {
		super("Partikel-Spur", "Partikel hinter dir, z. B. Herzen oder Flammen", Category.COSMETICS, false);
	}

	private SimpleParticleType particle() {
		return switch (type.get()) {
			case "Flammen" -> ParticleTypes.FLAME;
			case "Seelenflammen" -> ParticleTypes.SOUL_FIRE_FLAME;
			case "Noten" -> ParticleTypes.NOTE;
			case "Endstab" -> ParticleTypes.END_ROD;
			case "Kirschblüten" -> ParticleTypes.CHERRY_LEAVES;
			case "Glitzer" -> ParticleTypes.HAPPY_VILLAGER;
			case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
			case "Funken" -> ParticleTypes.ENCHANTED_HIT;
			default -> ParticleTypes.HEART;
		};
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		if (!isEnabled() || p == null || mc.level == null || p.isSpectator()) {
			return;
		}
		double dx = p.getX() - p.xo;
		double dz = p.getZ() - p.zo;
		boolean moving = dx * dx + dz * dz > 0.0004;
		if (onlyMoving.isOn() && !moving) {
			return;
		}
		// hearts and notes look best when they appear less often
		boolean slow = type.is("Herzen") || type.is("Noten") || type.is("Totem");
		if (slow && (counter++ % 3) != 0) {
			return;
		}
		for (int i = 0; i < amount.getInt(); i++) {
			double x = p.getX() - dx * 2 + (random.nextDouble() - 0.5) * 0.5;
			double y = p.getY() + 0.1 + random.nextDouble() * 0.3;
			double z = p.getZ() - dz * 2 + (random.nextDouble() - 0.5) * 0.5;
			mc.level.addParticle(particle(), x, y, z, 0.0, 0.02, 0.0);
		}
	}
}
