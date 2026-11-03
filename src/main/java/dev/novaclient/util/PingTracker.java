package dev.novaclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;

/**
 * Measures the real round trip time to the server with ping/pong packets (like the F3 network chart).
 * The latency in the tab list is only updated by the server every few seconds and many servers fake it,
 * so the HUD uses this measurement and falls back to the tab list value only if the server never answers.
 */
public final class PingTracker {
	private static final int INTERVAL_TICKS = 20;
	private static final int SAMPLES = 5;

	private static final long[] samples = new long[SAMPLES];
	private static int count;
	private static int next;
	private static int ticks;
	private static volatile long lastPong;
	private static ClientPacketListener lastConnection;

	private PingTracker() {
	}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		ClientPacketListener connection = mc.getConnection();
		if (connection != lastConnection) {
			lastConnection = connection;
			reset();
		}
		if (connection == null || mc.player == null) {
			return;
		}
		if (++ticks >= INTERVAL_TICKS) {
			ticks = 0;
			connection.send(new ServerboundPingRequestPacket(Util.getMillis()));
		}
	}

	/** Called for every pong from the server (also the ones requested by the F3 network chart). */
	public static synchronized void onPong(long sentMillis) {
		long rtt = Util.getMillis() - sentMillis;
		if (rtt < 0 || rtt > 60_000) {
			return;
		}
		samples[next] = rtt;
		next = (next + 1) % SAMPLES;
		count = Math.min(SAMPLES, count + 1);
		lastPong = Util.getMillis();
	}

	private static synchronized void reset() {
		count = 0;
		next = 0;
		ticks = INTERVAL_TICKS;
		lastPong = 0;
	}

	/** Measured ping in ms, -1 if nothing was measured recently. */
	public static synchronized int measured() {
		if (count == 0 || Util.getMillis() - lastPong > 10_000) {
			return -1;
		}
		long sum = 0;
		for (int i = 0; i < count; i++) {
			sum += samples[i];
		}
		return (int) Math.round(sum / (double) count);
	}

	/** Best known ping of the local player: measured, else the (server reported) tab list value, else -1. */
	public static int current(Minecraft mc) {
		int m = measured();
		if (m >= 0) {
			return m;
		}
		ClientPacketListener connection = mc.getConnection();
		if (connection == null || mc.player == null) {
			return -1;
		}
		PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
		return info == null ? -1 : Math.max(0, info.getLatency());
	}
}
