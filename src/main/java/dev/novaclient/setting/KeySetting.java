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
