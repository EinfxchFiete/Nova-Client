package dev.novaclient.module.hud;

import dev.novaclient.setting.BoolSetting;
import dev.novaclient.util.CombatTracker;
import dev.novaclient.util.Render;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Shows name, health and distance of the entity you last hit. */
public class TargetHudModule extends HudModule {
	private static final int W = 120;
	private static final int H = 30;

	public final BoolSetting showDistance = add(new BoolSetting("Entfernung anzeigen", true));

	private float shownHealth = -1;

	public TargetHudModule() {
		super("Ziel-HUD", "Name, Leben und Entfernung deines Gegners", false, Anchor.CENTER, 0, Anchor.CENTER, 50);
	}

	private Entity target(boolean editor) {
		Entity t = CombatTracker.lastTarget();
		return t != null ? t : editor ? mc().player : null;
	}

	@Override
	public int getWidth(boolean editor) {
		return target(editor) == null ? 0 : W;
	}

	@Override
	public int getHeight(boolean editor) {
		return target(editor) == null ? 0 : H;
	}

	@Override
	protected void renderContent(GuiGraphicsExtractor g, int x, int y, boolean editor) {
		Entity t = target(editor);
		if (t == null) {
			return;
		}
		int accent = ThemeModule.accent();
		g.fill(x, y, x + 2, y + H, accent);
		text(g, font().plainSubstrByWidth(t.getDisplayName().getString(), W - 12), x + 7, y + 4, 0xFFFFFFFF);
		if (t instanceof LivingEntity living) {
			float max = Math.max(1, living.getMaxHealth());
			float hp = Math.max(0, living.getHealth());
			if (shownHealth < 0 || Math.abs(shownHealth - hp) > max) {
				shownHealth = hp;
			}
			shownHealth += (hp - shownHealth) * 0.2F;
			int barW = W - 14;
			int filled = (int) (barW * Math.min(1, shownHealth / max));
			Render.rounded(g, x + 7, y + 16, x + 7 + barW, y + 20, 0xFF2A2A34);
			int color = hp / max > 0.5F ? 0xFF55DD66 : hp / max > 0.25F ? 0xFFEEDD44 : 0xFFEE4444;
			if (filled >= 4) {
				Render.rounded(g, x + 7, y + 16, x + 7 + filled, y + 20, color);
			}
			String hpText = String.format(Locale.ROOT, "%.1f ❤", hp);
			g.pose().pushMatrix();
			g.pose().translate(x + 7, y + 22.5F);
			g.pose().scale(0.75F, 0.75F);
			g.text(font(), hpText, 0, 0, 0xFFE0E0E8, false);
			if (showDistance.isOn() && mc().player != null && t != mc().player) {
				String d = String.format(Locale.ROOT, "%.1f m", mc().player.distanceTo(t));
				g.text(font(), d, (int) ((W - 14) / 0.75F) - font().width(d), 0, 0xFFA0A0B0, false);
			}
			g.pose().popMatrix();
		}
	}
}
