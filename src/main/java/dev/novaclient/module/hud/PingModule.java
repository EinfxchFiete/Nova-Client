package dev.novaclient.module.hud;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PingModule extends TextHudModule {
	public PingModule() {
		super("Ping", "Zeigt deine Latenz zum Server", true, Anchor.START, 2, Anchor.START, 18);
	}

	@Override
	protected String label() {
		return "Ping";
	}

	@Override
	protected String value() {
		ClientPacketListener connection = mc().getConnection();
		if (connection == null || mc().player == null) {
			return "0 ms";
		}
		PlayerInfo info = connection.getPlayerInfo(mc().player.getUUID());
		return (info == null ? 0 : info.getLatency()) + " ms";
	}
}
