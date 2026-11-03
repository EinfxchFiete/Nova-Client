package dev.novaclient.module.hud;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/** Durability of the item in your main hand as numbers, coloured green to red. */
public class HeldDurabilityModule extends TextHudModule {
	public HeldDurabilityModule() {
		super("Item-Haltbarkeit", "Haltbarkeit des Gegenstands in deiner Hand", false, Anchor.CENTER, 0, Anchor.END, 64);
	}

	private ItemStack held() {
		LocalPlayer p = mc().player;
		return p == null ? ItemStack.EMPTY : p.getMainHandItem();
	}

	@Override
	public int getWidth(boolean editor) {
		return !editor && !held().isDamageableItem() ? 0 : super.getWidth(editor);
	}

	@Override
	public int getHeight(boolean editor) {
		return !editor && !held().isDamageableItem() ? 0 : super.getHeight(editor);
	}

	@Override
	protected String label() {
		return "Haltbarkeit";
	}

	@Override
	protected String value() {
		ItemStack s = held();
		if (!s.isDamageableItem()) {
			return "1561 / 1561";
		}
		int max = s.getMaxDamage();
		int left = max - s.getDamageValue();
		return left + " / " + max + " (" + Math.round(left * 100F / max) + "%)";
	}
}
