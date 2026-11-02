package dev.novaclient.module.hud;

import net.minecraft.client.multiplayer.ServerData;

public class ServerAddressModule extends TextHudModule {
	public ServerAddressModule() {
		super("Server-IP", "Zeigt die aktuelle Server-Adresse", false, Anchor.END, 2, Anchor.START, 18);
	}

	@Override
	protected String label() {
		return "Server";
	}

	@Override
	protected String value() {
		ServerData server = mc().getCurrentServer();
		if (server == null || server.ip == null) {
			return "Einzelspieler";
		}
		return server.ip;
	}
}
