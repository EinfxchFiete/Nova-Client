package dev.novaclient.module.hud;

public class SessionTimeModule extends TextHudModule {
	private Object lastLevel;
	private long joinedAt = System.currentTimeMillis();

	public SessionTimeModule() {
		super("Spielzeit", "Wie lange du schon in dieser Welt/auf diesem Server bist", false, Anchor.END, 2, Anchor.START, 98);
	}

	@Override
	public void tick() {
		Object level = mc().level;
		if (level != null && lastLevel == null) {
			joinedAt = System.currentTimeMillis();
		}
		lastLevel = level;
	}

	@Override
	protected String label() {
		return "Spielzeit";
	}

	@Override
	protected String value() {
		long s = mc().level == null ? 0 : (System.currentTimeMillis() - joinedAt) / 1000L;
		long h = s / 3600;
		long m = (s % 3600) / 60;
		long sec = s % 60;
		return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
	}
}
