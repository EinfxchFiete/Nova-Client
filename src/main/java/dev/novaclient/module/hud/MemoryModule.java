package dev.novaclient.module.hud;

public class MemoryModule extends TextHudModule {
	public MemoryModule() {
		super("Arbeitsspeicher", "RAM-Auslastung von Minecraft", false, Anchor.END, 2, Anchor.START, 50);
	}

	@Override
	protected String label() {
		return "RAM";
	}

	@Override
	protected String value() {
		Runtime rt = Runtime.getRuntime();
		long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
		long max = rt.maxMemory() / 1048576L;
		int pct = max <= 0 ? 0 : (int) (used * 100 / max);
		return pct + "% (" + used + "/" + max + " MB)";
	}
}
