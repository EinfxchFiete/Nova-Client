package dev.novaclient.module;

import dev.novaclient.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class Module {
	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final boolean enabledByDefault;
	private boolean enabled;
	/** SDL scancode (InputConstants.KEY_*), 0 = not bound. */
	private int keybind;

	protected Module(String name, String description, Category category, boolean enabledByDefault) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.enabledByDefault = enabledByDefault;
		this.enabled = enabledByDefault;
	}

	protected static Minecraft mc() {
		return Minecraft.getInstance();
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(settings);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	/** Sets the state from the config without running enable/disable side effects twice. */
	public void loadEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public int getKeybind() {
		return keybind;
	}

	public void setKeybind(int keybind) {
		this.keybind = Math.max(0, keybind);
	}

	/** Restores enabled state, keybind and all settings to their defaults (with enable/disable side effects). */
	public void resetToDefaults() {
		for (Setting<?> s : settings) {
			s.reset();
		}
		keybind = 0;
		setEnabled(enabledByDefault);
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Called every client tick, regardless of state. */
	public void tick() {
	}
}
