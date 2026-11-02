package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArmorStatusModule extends HudModule {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int ROW = 17;

	public final BoolSetting showMainHand = add(new BoolSetting("Item in der Hand", true));
	public final BoolSetting percent = add(new BoolSetting("Prozent statt Zahl", false));

	public ArmorStatusModule() {
		super("Rüstungsstatus", "Rüstung mit Haltbarkeit", true, Anchor.END, 2, Anchor.END, 2);
	}

	private ItemStack[] stacks(boolean editor) {
		LocalPlayer p = mc().player;
		int n = SLOTS.length + (showMainHand.isOn() ? 1 : 0);
		ItemStack[] out = new ItemStack[n];
		for (int i = 0; i < SLOTS.length; i++) {
			out[i] = p == null ? ItemStack.EMPTY : p.getItemBySlot(SLOTS[i]);
		}
		if (showMainHand.isOn()) {
			out[n - 1] = p == null ? ItemStack.EMPTY : p.getItemBySlot(EquipmentSlot.MAINHAND);
		}
		if (editor) {
			boolean allEmpty = true;
			for (ItemStack s : out) {
				if (!s.isEmpty()) {
					allEmpty = false;
					break;
				}
			}
			if (allEmpty) {
				out[0] = new ItemStack(Items.DIAMOND_HELMET);
				out[1] = new ItemStack(Items.DIAMOND_CHESTPLATE);
				out[2] = new ItemStack(Items.DIAMOND_LEGGINGS);
				out[3] = new ItemStack(Items.DIAMOND_BOOTS);
			}
		}
		return out;
	}

	private String info(ItemStack s) {
		if (s.isEmpty()) {
			return "";
		}
		if (s.isDamageableItem() && s.getMaxDamage() > 0) {
			int left = s.getMaxDamage() - s.getDamageValue();
			if (percent.isOn()) {
				return (left * 100 / s.getMaxDamage()) + "%";
			}
			return Integer.toString(left);
		}
		return s.getCount() > 1 ? Integer.toString(s.getCount()) : "";
	}

	private static int durabilityColor(ItemStack s) {
		if (!s.isDamageableItem() || s.getMaxDamage() <= 0) {
			return 0xFFFFFFFF;
		}
		float f = (s.getMaxDamage() - s.getDamageValue()) / (float) s.getMaxDamage();
		if (f > 0.6f) {
			return 0xFF55FF55;
		}
		if (f > 0.3f) {
			return 0xFFFFFF55;
		}
		return 0xFFFF5555;
	}

	@Override
	public int getWidth(boolean editor) {
		int w = 0;
		for (ItemStack s : stacks(editor)) {
			w = Math.max(w, font().width(info(s)));
		}
		return 16 + 4 + Math.max(w, font().width("100%")) + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return stacks(editor).length * ROW + PADDING * 2 - 1;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		ItemStack[] stacks = stacks(editor);
		int ty = y + PADDING;
		for (ItemStack s : stacks) {
			if (!s.isEmpty()) {
				g.item(s, x + PADDING, ty);
				text(g, info(s), x + PADDING + 20, ty + 4, durabilityColor(s));
			}
			ty += ROW;
		}
	}
}
