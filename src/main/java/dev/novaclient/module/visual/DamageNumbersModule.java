package dev.novaclient.module.visual;

import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Floating numbers when a nearby entity loses (red) or gains (green) health. Uses the health the client already knows. */
public class DamageNumbersModule extends Module {
	private static DamageNumbersModule instance;
	private static final long LIFE_MS = 1200;
	private static final double RANGE = 24;

	public final BoolSetting healing = add(new BoolSetting("Heilung anzeigen", true));
	public final BoolSetting self = add(new BoolSetting("Eigenen Schaden anzeigen", false));

	private record Popup(Vec3 pos, String text, int color, long start, double drift) {
	}

	private final Map<Integer, Float> lastHealth = new HashMap<>();
	private final List<Popup> popups = new ArrayList<>();

	public DamageNumbersModule() {
		super("Schadenszahlen", "Zeigt Schaden und Heilung als Zahlen über Spielern und Mobs", Category.VISUAL, false);
		instance = this;
	}

	@Override
	public void tick() {
		Minecraft mc = mc();
		if (!isEnabled() || mc.level == null || mc.player == null) {
			lastHealth.clear();
			return;
		}
		Map<Integer, Float> now = new HashMap<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity living) || e.distanceToSqr(mc.player) > RANGE * RANGE || e.getId() < 0) {
				continue;
			}
			if (e == mc.player && !self.isOn()) {
				continue;
			}
			float h = living.getHealth();
			now.put(e.getId(), h);
			Float before = lastHealth.get(e.getId());
			if (before == null || Math.abs(before - h) < 0.01F) {
				continue;
			}
			float diff = h - before;
			if (diff > 0 && !healing.isOn()) {
				continue;
			}
			String text = (diff < 0 ? "-" : "+") + format(Math.abs(diff));
			Vec3 pos = new Vec3(e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ());
			popups.add(new Popup(pos, text, diff < 0 ? 0xFFFF5555 : 0xFF55FF55, System.currentTimeMillis(), (Math.random() - 0.5) * 0.6));
		}
		lastHealth.clear();
		lastHealth.putAll(now);
		if (popups.size() > 40) {
			popups.subList(0, popups.size() - 40).clear();
		}
	}

	private static String format(float v) {
		return v == Math.round(v) ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
	}

	/** How many popups the last frame drew (tests). */
	public static volatile int drawnLastFrame;

	/** Number of popups on screen (tests). */
	public int activePopups() {
		return popups.size();
	}

	/** Draws the popups (called from the HUD). */
	public static void render(GuiGraphicsExtractor g, int screenW, int screenH) {
		DamageNumbersModule m = instance;
		if (m == null || !m.isEnabled() || m.popups.isEmpty()) {
			drawnLastFrame = 0;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		int drawn = 0;
		Iterator<Popup> it = m.popups.iterator();
		while (it.hasNext()) {
			Popup p = it.next();
			float age = (now - p.start()) / (float) LIFE_MS;
			if (age >= 1) {
				it.remove();
				continue;
			}
			Vec3 world = p.pos().add(p.drift() * age, age * 0.8, 0);
			Vec3 ndc = mc.gameRenderer.projectPointToScreen(world);
			if (ndc.z > 1.0 || Math.abs(ndc.x) > 1.1 || Math.abs(ndc.y) > 1.1) {
				continue;
			}
			drawn++;
			int sx = (int) Math.round((ndc.x + 1) / 2 * screenW);
			int sy = (int) Math.round((1 - ndc.y) / 2 * screenH);
			int alpha = (int) (255 * (1 - age * age));
			int color = (alpha << 24) | (p.color() & 0xFFFFFF);
			float scale = 1.3F - age * 0.4F;
			g.pose().pushMatrix();
			g.pose().translate(sx, sy);
			g.pose().scale(scale, scale);
			g.text(mc.font, p.text(), -mc.font.width(p.text()) / 2, -4, color, true);
			g.pose().popMatrix();
		}
		drawnLastFrame = drawn;
	}
}
