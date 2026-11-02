package dev.novaclient.setting;

import com.google.gson.JsonElement;

public abstract class Setting<T> {
	private final String name;
	private final T defaultValue;
	protected T value;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public void reset() {
		set(defaultValue);
	}

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement element);
}
