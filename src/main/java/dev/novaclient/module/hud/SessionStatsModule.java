package dev.novaclient.module.hud;

import dev.novaclient.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Statistics of this session in the current world: distance walked and deaths. */
public class SessionStatsModule extends TextHudModule {
	public final ModeSetting show = add(new ModeSetting("Anzeige", "Alles", "Alles", "Strecke", "Tode"));
	private Object connection;
	private double distance;
	private int deaths;
	private boolean wasDead;
	private double lastX;
	private double lastZ;
	private boolean hasLast;

	public SessionStatsModule() {
		super("Sitzungsstatistik", "Wie weit du in dieser Sitzung gelaufen bist und wie oft du gestorben bist", false,
			Anchor.START, 2, Anchor.START, 110);
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) {
			hasLast = false;
			return;
		}
		// a new world or server starts a new session (a dimension change keeps it)
		if (mc.getConnection() != connection) {
			connection = mc.getConnection();
			distance = 0;
			deaths = 0;
			wasDead = false;
			hasLast = false;
		}
		boolean dead = p.isDeadOrDying();
		if (dead && !wasDead) {
			deaths++;
		}
		wasDead = dead;
		if (hasLast && !dead) {
			double dx = p.getX() - lastX;
			double dz = p.getZ() - lastZ;
			double d = Math.sqrt(dx * dx + dz * dz);
			// teleports and dimension changes do not count
			if (d < 12) {
				distance += d;
			}
		}
		lastX = p.getX();
		lastZ = p.getZ();
		hasLast = true;
	}

	public double distance() {
		return distance;
	}

	public int deaths() {
		return deaths;
	}

	private String distanceText() {
		return distance >= 1000 ? String.format(java.util.Locale.ROOT, "%.2f km", distance / 1000) : Math.round(distance) + " m";
	}

	@Override
	protected String label() {
		return switch (show.get()) {
			case "Strecke" -> "Strecke";
			case "Tode" -> "Tode";
			default -> "Sitzung";
		};
	}

	@Override
	protected String value() {
		return switch (show.get()) {
			case "Strecke" -> distanceText();
			case "Tode" -> Integer.toString(deaths);
			default -> distanceText() + " · " + deaths + (deaths == 1 ? " Tod" : " Tode");
		};
	}
}
