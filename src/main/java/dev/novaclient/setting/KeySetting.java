package dev.novaclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.novaclient.util.KeyState;

/** A key or mouse button, see {@link KeyState} for the encoding. 0 = none. */
public class KeySetting extends Setting<Integer> {
	public KeySetting(String name, int defaultKey) {
		super(name, defaultKey);
	}

	public int code() {
		return value;
	}

	private int seenPresses = -1;

	/** True once for every new press since the last call (only counts presses while no screen is open). */
	public boolean consumePress(boolean inGame) {
		int now = KeyState.presses(value);
		if (seenPresses < 0) {
			seenPresses = now;
			return false;
		}
		boolean pressed = now != seenPresses && inGame;
		seenPresses = now;
		return pressed;
	}

	public boolean isDown() {
		return KeyState.isDown(value);
	}

	public String display() {
		return KeyState.name(value);
	}

	@Override
	public void set(Integer v) {
		this.value = v == null ? 0 : Math.max(0, v);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			set(element.getAsInt());
		}
	}
}
