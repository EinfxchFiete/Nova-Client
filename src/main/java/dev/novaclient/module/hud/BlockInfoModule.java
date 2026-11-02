package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Name (and position) of the block you are looking at. */
public class BlockInfoModule extends HudModule {
	public final BoolSetting showPos = add(new BoolSetting("Position anzeigen", true));

	public BlockInfoModule() {
		super("Block-Info", "Name und Position des Blocks, den du anschaust", false, Anchor.CENTER, 0, Anchor.START, 42);
	}

	private String[] lines(boolean editor) {
		if (mc().hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && mc().level != null) {
			BlockPos pos = hit.getBlockPos();
			BlockState state = mc().level.getBlockState(pos);
			String name = state.getBlock().getName().getString();
			return showPos.isOn() ? new String[] {name, pos.getX() + ", " + pos.getY() + ", " + pos.getZ()} : new String[] {name};
		}
		return editor ? new String[] {"Grasblock", "12, 64, -30"} : new String[0];
	}

	@Override
	public int getWidth(boolean editor) {
		int w = 0;
		for (String s : lines(editor)) {
			w = Math.max(w, font().width(s));
		}
		return w == 0 ? 0 : w + PADDING * 2;
	}

	@Override
	public int getHeight(boolean editor) {
		int n = lines(editor).length;
		return n == 0 ? 0 : n * (font().lineHeight + 1) + PADDING * 2 - 1;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		String[] lines = lines(editor);
		for (int i = 0; i < lines.length; i++) {
			int w = font().width(lines[i]);
			int tx = x + (getWidth(editor) - w) / 2;
			text(g, lines[i], tx, y + PADDING + i * (font().lineHeight + 1), i == 0 ? 0xFFFFFFFF : 0xFFA0A0B0);
		}
	}
}
