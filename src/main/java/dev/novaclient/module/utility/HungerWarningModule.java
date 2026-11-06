package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.util.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

/** Warns once when your hunger bar gets low (again after you have eaten). */
public class HungerWarningModule extends Module {
	public final NumberSetting below = add(new NumberSetting("Warnen ab (Hunger-Punkte)", 6, 1, 19, 1));
	public final BoolSetting sound = add(new BoolSetting("Ton", true));
	private boolean warned;

	public HungerWarningModule() {
		super("Hunger-Warnung", "Meldet sich, wenn du Hunger bekommst (bevor du nicht mehr sprinten kannst)", Category.UTILITY, false);
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (!isEnabled() || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
			return;
		}
		int food = mc.player.getFoodData().getFoodLevel();
		if (food <= below.getInt()) {
			if (!warned) {
				warned = true;
				Notifications.push("Du hast Hunger! (" + food + "/20) – iss etwas", 0xFFFFB74D);
				if (sound.isOn()) {
					mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.7F, 0.8F);
				}
			}
		} else if (food > below.getInt() + 1) {
			warned = false;
		}
	}
}
