package dev.novaclient.module.hud;

import dev.novaclient.setting.ModeSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Counts an item in your inventory: totems or whatever you hold. */
public class ItemCounterModule extends HudModule {
	public final ModeSetting mode;
	private ItemStack icon;
	private Item iconItem;

	public ItemCounterModule(String name, String description, String defaultMode, Anchor ax, int ox, Anchor ay, int oy) {
		super(name, description, false, ax, ox, ay, oy);
		this.mode = add(new ModeSetting("Zählen", defaultMode, "Totems", "Gehaltenes Item", "Goldäpfel", "Enderperlen"));
	}

	private Item item() {
		LocalPlayer p = mc().player;
		return switch (mode.get()) {
			case "Totems" -> Items.TOTEM_OF_UNDYING;
			case "Goldäpfel" -> Items.GOLDEN_APPLE;
			case "Enderperlen" -> Items.ENDER_PEARL;
			default -> p == null || p.getMainHandItem().isEmpty() ? Items.AIR : p.getMainHandItem().getItem();
		};
	}

	private int count(Item item) {
		LocalPlayer p = mc().player;
		if (p == null || item == Items.AIR) {
			return 0;
		}
		Inventory inv = p.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.getItem() == item) {
				n += s.getCount();
			}
		}
		return n;
	}

	@Override
	public int getWidth(boolean editor) {
		Item item = item();
		if (item == Items.AIR && !editor) {
			return 0;
		}
		return 16 + 3 + font().width(Integer.toString(Math.max(count(item), 64))) + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return item() == Items.AIR && !editor ? 0 : 16 + PADDING * 2;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		Item item = item();
		if (item == Items.AIR) {
			item = Items.TOTEM_OF_UNDYING;
		}
		if (icon == null || iconItem != item) {
			icon = new ItemStack(item);
			iconItem = item;
		}
		int n = count(item);
		g.item(icon, x + PADDING, y + PADDING);
		text(g, Integer.toString(n), x + PADDING + 19, y + PADDING + 4, n == 0 ? 0xFFFF5555 : 0xFFFFFFFF);
	}
}
