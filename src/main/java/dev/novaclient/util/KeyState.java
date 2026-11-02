package dev.novaclient.util;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which keys and mouse buttons are physically held, based on the game's input events.
 * Keyboard keys use their SDL scancode; mouse buttons are stored as {@link #MOUSE_OFFSET} + button.
 */
public final class KeyState {
	public static final int MOUSE_OFFSET = 1000;
	private static final Set<Integer> DOWN = new HashSet<>();

	private KeyState() {
	}

	public static void key(int key, boolean pressed) {
		set(key, pressed);
	}

	public static void mouse(int button, boolean pressed) {
		set(MOUSE_OFFSET + button, pressed);
	}

	private static void set(int code, boolean pressed) {
		if (pressed) {
			DOWN.add(code);
		} else {
			DOWN.remove(code);
		}
	}

	public static boolean isDown(int code) {
		return code > 0 && DOWN.contains(code);
	}

	public static void releaseAll() {
		DOWN.clear();
	}

	public static String name(int code) {
		if (code <= 0) {
			return "Keine";
		}
		if (code >= MOUSE_OFFSET) {
			int b = code - MOUSE_OFFSET;
			return switch (b) {
				case InputConstants.MOUSE_BUTTON_LEFT -> "Linksklick";
				case InputConstants.MOUSE_BUTTON_MIDDLE -> "Mausrad-Klick";
				case InputConstants.MOUSE_BUTTON_RIGHT -> "Rechtsklick";
				default -> "Maustaste " + b;
			};
		}
		return InputConstants.Type.KEYBOARD.getOrCreate(code).getDisplayName().getString();
	}
}
