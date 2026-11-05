package dev.novaclient.util;

/**
 * Estimates the server's ticks per second from the time packets: the server sends its game time about once per second,
 * so game ticks passed / real seconds passed is the TPS (20 = no lag).
 */
public final class TpsTracker {
	private static final int SAMPLES = 6;
	private static final float[] TPS = new float[SAMPLES];
	private static int count;
	private static int next;
	private static long lastGameTime = Long.MIN_VALUE;
	private static long lastNanos;

	private TpsTracker() {
	}

	/** Called on the main thread for every time packet. */
	public static synchronized void onTime(long gameTime) {
		long now = System.nanoTime();
		if (lastGameTime != Long.MIN_VALUE) {
			long ticks = gameTime - lastGameTime;
			double seconds = (now - lastNanos) / 1.0E9;
			// ignore jumps (/time set, world change) and packets that came in a burst
			if (ticks > 0 && ticks <= 200 && seconds > 0.25) {
				TPS[next] = (float) Math.min(20.0, ticks / seconds);
				next = (next + 1) % SAMPLES;
				count = Math.min(SAMPLES, count + 1);
			}
		}
		lastGameTime = gameTime;
		lastNanos = now;
	}

	/** Average TPS, or -1 while unknown (no or old data). */
	public static synchronized float tps() {
		if (count == 0 || System.nanoTime() - lastNanos > 6_000_000_000L) {
			return -1;
		}
		float sum = 0;
		for (int i = 0; i < count; i++) {
			sum += TPS[i];
		}
		return sum / count;
	}

	public static synchronized void reset() {
		count = 0;
		next = 0;
		lastGameTime = Long.MIN_VALUE;
	}
}
