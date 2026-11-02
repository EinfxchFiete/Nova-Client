package dev.novaclient.module.utility;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.NumberSetting;
import dev.novaclient.util.Notifications;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Warns you before your tool or armor breaks. */
public class DurabilityWarningModule extends Module {
	private static final EquipmentSlot[] SLOTS = {
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	public final NumberSetting threshold = add(new NumberSetting("Warnen unter (%)", 10, 1, 50, 1));

	private final Map<EquipmentSlot, Boolean> warned = new HashMap<>();

	public DurabilityWarningModule() {
		super("Haltbarkeitswarnung", "Warnt, bevor Werkzeug oder Rüstung kaputtgeht", Category.UTILITY, true);
	}

	@Override
	public void tick() {
		LocalPlayer p = mc().player;
		if (!isEnabled() || p == null) {
			warned.clear();
			return;
		}
		for (EquipmentSlot slot : SLOTS) {
			ItemStack s = p.getItemBySlot(slot);
			boolean low = false;
			if (!s.isEmpty() && s.isDamageableItem() && s.getMaxDamage() > 0) {
				int left = s.getMaxDamage() - s.getDamageValue();
				low = left * 100 < threshold.get() * s.getMaxDamage();
				if (low && !warned.getOrDefault(slot, false)) {
					Notifications.push(s.getHoverName().getString() + ": nur noch " + left + " Haltbarkeit!", 0xFFFF5555);
				}
			}
			warned.put(slot, low);
		}
	}
}
