package dev.novaclient.module.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.novaclient.setting.KeySetting;
import dev.novaclient.util.Notifications;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** One quick waypoint: press the key to mark your position, the HUD shows distance and direction. */
public class WaypointModule extends TextHudModule {
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	public final KeySetting setKey = add(new KeySetting("Wegpunkt-Taste", InputConstants.KEY_B));

	private boolean has;
	private double wx;
	private double wy;
	private double wz;
	private String dimension = "";

	public WaypointModule() {
		super("Wegpunkt", "Taste B markiert deine Position, das HUD zeigt den Weg zurück", false, Anchor.CENTER, 0, Anchor.START, 78);
	}

	@Override
	public void tick() {
		LocalPlayer p = mc().player;
		if (setKey.consumePress(mc().gui.screen() == null) && isEnabled() && p != null) {
			has = true;
			wx = p.getX();
			wy = p.getY();
			wz = p.getZ();
			dimension = p.level().dimension().identifier().toString();
			Notifications.push("Wegpunkt gesetzt: " + p.blockPosition().getX() + ", " + p.blockPosition().getY() + ", " + p.blockPosition().getZ(), 0);
		}
	}

	public boolean hasWaypoint() {
		return has;
	}

	@Override
	protected String label() {
		return "Wegpunkt";
	}

	@Override
	protected String value() {
		LocalPlayer p = mc().player;
		if (!has || p == null) {
			return "Taste " + setKey.display() + " zum Setzen";
		}
		if (!p.level().dimension().identifier().toString().equals(dimension)) {
			return "andere Dimension";
		}
		double dx = wx - p.getX();
		double dz = wz - p.getZ();
		int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz + (wy - p.getY()) * (wy - p.getY())));
		float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float rel = Mth.wrapDegrees(target - p.getYRot());
		String arrow = ARROWS[Math.floorMod(Math.round(rel / 45F), 8)];
		return dist + " m " + arrow;
	}
}
