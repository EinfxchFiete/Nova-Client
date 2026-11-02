package dev.novaclient.module.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArrowCounterModule extends HudModule {
	/** Created lazily: item components are not bound yet while mods initialise. */
	private ItemStack icon;

	public ArrowCounterModule() {
		super("Pfeile", "Anzahl der Pfeile im Inventar", false, Anchor.END, 2, Anchor.START, 114);
	}

	private int count() {
		LocalPlayer p = mc().player;
		if (p == null) {
			return 0;
		}
		Inventory inv = p.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.getItem() == Items.ARROW || s.getItem() == Items.SPECTRAL_ARROW || s.getItem() == Items.TIPPED_ARROW) {
				n += s.getCount();
			}
		}
		return n;
	}

	@Override
	public int getWidth(boolean editor) {
		return 16 + 3 + font().width(Integer.toString(editor ? Math.max(64, count()) : count())) + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		return 16 + PADDING * 2;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		int n = count();
		if (editor && n == 0) {
			n = 64;
		}
		if (icon == null) {
			icon = new ItemStack(Items.ARROW);
		}
		g.item(icon, x + PADDING, y + PADDING);
		text(g, Integer.toString(n), x + PADDING + 19, y + PADDING + 4, n == 0 ? 0xFFFF5555 : 0xFFFFFFFF);
	}
}
