package dev.novaclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

/** One choice out of a fixed list of options, cycled by clicking. */
public class ModeSetting extends Setting<String> {
	private final List<String> modes;

	public ModeSetting(String name, String defaultValue, String... modes) {
		super(name, defaultValue);
		this.modes = List.of(modes);
		if (!this.modes.contains(defaultValue)) {
			throw new IllegalArgumentException("Default " + defaultValue + " not in " + this.modes);
		}
	}

	public List<String> getModes() {
		return modes;
	}

	public int index() {
		return Math.max(0, modes.indexOf(value));
	}

	public boolean is(String mode) {
		return value.equals(mode);
	}

	@Override
	public void set(String newValue) {
		if (modes.contains(newValue)) {
			this.value = newValue;
		}
	}

	public void cycle(int direction) {
		int i = Math.floorMod(index() + (direction >= 0 ? 1 : -1), modes.size());
		this.value = modes.get(i);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
			set(element.getAsString());
		}
	}
}
