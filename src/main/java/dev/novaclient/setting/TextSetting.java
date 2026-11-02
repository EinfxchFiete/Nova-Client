package dev.novaclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A short text, edited directly in the menu. */
public class TextSetting extends Setting<String> {
	private final int maxLength;

	public TextSetting(String name, String defaultValue, int maxLength) {
		super(name, defaultValue);
		this.maxLength = maxLength;
	}

	public int getMaxLength() {
		return maxLength;
	}

	@Override
	public void set(String v) {
		String s = v == null ? "" : v.replace("\n", " ");
		this.value = s.length() > maxLength ? s.substring(0, maxLength) : s;
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
