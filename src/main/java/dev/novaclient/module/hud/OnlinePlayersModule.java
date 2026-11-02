package dev.novaclient.module.hud;

public class OnlinePlayersModule extends TextHudModule {
	public OnlinePlayersModule() {
		super("Spieler online", "Anzahl der Spieler auf dem Server", false, Anchor.END, 2, Anchor.START, 234);
	}

	@Override
	protected String label() {
		return "Online";
	}

	@Override
	protected String value() {
		return mc().getConnection() == null ? "1" : Integer.toString(mc().getConnection().getListedOnlinePlayers().size());
	}
}
