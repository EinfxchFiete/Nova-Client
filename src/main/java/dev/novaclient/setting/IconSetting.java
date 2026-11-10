package dev.novaclient.setting;

import dev.novaclient.cosmetics.NameIcons;
import java.util.List;

/** Like a {@link ModeSetting}, but the choices are the name icons that exist right now (built-in + your own files). */
public class IconSetting extends ModeSetting {
	public IconSetting(String name) {
		super(name, NameIcons.NONE, NameIcons.NONE);
	}

	@Override
	public List<String> getModes() {
		return NameIcons.names();
	}

	@Override
	public int index() {
		return Math.max(0, getModes().indexOf(value));
	}

	/** Any name is kept (the file may be added later); a missing icon simply shows nothing. */
	@Override
	public void set(String newValue) {
		if (newValue != null && !newValue.isEmpty() && newValue.length() <= 40) {
			this.value = newValue;
		}
	}

	@Override
	public void cycle(int direction) {
		List<String> modes = getModes();
		this.value = modes.get(Math.floorMod(index() + (direction >= 0 ? 1 : -1), modes.size()));
	}
}
