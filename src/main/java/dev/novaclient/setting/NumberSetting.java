package dev.novaclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;

	public NumberSetting(String name, double defaultValue, double min, double max, double step) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
	}

	@Override
	public void set(Double newValue) {
		double v = newValue == null || newValue.isNaN() ? min : newValue;
		v = Math.max(min, Math.min(max, v));
		if (step > 0) {
			v = Math.round((v - min) / step) * step + min;
			v = Math.max(min, Math.min(max, v));
		}
		this.value = v;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	public float getFloat() {
		return value.floatValue();
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	public String format() {
		if (step >= 1) {
			return Integer.toString(getInt());
		}
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			set(element.getAsDouble());
		}
	}
}
