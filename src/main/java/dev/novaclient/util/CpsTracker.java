package dev.novaclient.util;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Counts mouse clicks of the last second. Only accessed from the main thread.
 * Note: since Minecraft 26.x (SDL input) the left button is 1 and the right button is 3.
 */
public final class CpsTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();

	private CpsTracker() {
	}

	public static void onClick(int button) {
		long now = System.currentTimeMillis();
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			LEFT.addLast(now);
		} else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
			RIGHT.addLast(now);
		}
	}

	public static int left() {
		return count(LEFT);
	}

	public static int right() {
		return count(RIGHT);
	}

	private static int count(Deque<Long> clicks) {
		long cutoff = System.currentTimeMillis() - 1000L;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
			clicks.pollFirst();
		}
		return clicks.size();
	}
}
