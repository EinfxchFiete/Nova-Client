package dev.novaclient.module;

public enum Category {
	HUD("HUD"),
	VISUAL("Visuals"),
	UTILITY("Utility");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
