package dev.novaclient.cosmetics;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/** All cosmetics with their ids (used on the network) and German display names. */
public final class CosmeticRegistry {
	/** head cosmetic id -> display name */
	public static final Map<String, String> HEADS = new LinkedHashMap<>();
	/** wings id -> display name */
	public static final Map<String, String> WINGS = new LinkedHashMap<>();
	/** colour name -> RGB, "Regenbogen" is animated, "Normal" = untinted */
	public static final Map<String, Integer> COLORS = new LinkedHashMap<>();
	/** animated cape id -> display name */
	public static final Map<String, String> ANIMATED_CAPES = new LinkedHashMap<>();

	/** minimee position id -> display name */
	public static final Map<String, String> MINIMEE_MODES = new LinkedHashMap<>();

	public static final int CAPE_FRAMES = 16;

	static {
		HEADS.put("bandana", "Bandana");
		HEADS.put("crown", "Krone");
		HEADS.put("tophat", "Zylinder");
		HEADS.put("horns", "Teufelshörner");
		HEADS.put("catears", "Katzenohren");
		HEADS.put("halo", "Heiligenschein");

		WINGS.put("angel", "Engelsflügel");
		WINGS.put("dragon", "Drachenflügel");
		WINGS.put("butterfly", "Schmetterling");

		COLORS.put("Normal", 0xFFFFFF);
		COLORS.put("Rot", 0xE53935);
		COLORS.put("Orange", 0xFB8C00);
		COLORS.put("Gelb", 0xFDD835);
		COLORS.put("Grün", 0x43A047);
		COLORS.put("Türkis", 0x00ACC1);
		COLORS.put("Blau", 0x1E88E5);
		COLORS.put("Lila", 0x8E24AA);
		COLORS.put("Pink", 0xEC407A);
		COLORS.put("Schwarz", 0x303030);
		COLORS.put("Regenbogen", -1);

		ANIMATED_CAPES.put("anim:nova", "Nova Puls (animiert)");
		ANIMATED_CAPES.put("anim:galaxy", "Galaxie (animiert)");
		ANIMATED_CAPES.put("anim:fire", "Feuer (animiert)");
		ANIMATED_CAPES.put("anim:ocean", "Ozean (animiert)");
		ANIMATED_CAPES.put("anim:rainbow", "Regenbogen (animiert)");
		ANIMATED_CAPES.put("anim:matrix", "Matrix (animiert)");
		ANIMATED_CAPES.put("anim:aurora", "Polarlicht (animiert)");
		ANIMATED_CAPES.put("anim:lava", "Magma (animiert)");
		ANIMATED_CAPES.put("anim:storm", "Gewitter (animiert)");
		ANIMATED_CAPES.put("anim:sakura", "Blütenregen (animiert)");
		ANIMATED_CAPES.put("anim:ender", "Ender (animiert)");
		ANIMATED_CAPES.put("anim:snow", "Schneefall (animiert)");
		ANIMATED_CAPES.put("anim:neon", "Neon (animiert)");

		MINIMEE_MODES.put("neck", "Kette");
		MINIMEE_MODES.put("pocket", "Hosentasche");
		MINIMEE_MODES.put("head", "Auf dem Kopf");
	}

	private CosmeticRegistry() {
	}

	/** ARGB tint for a colour name; animated colours change over time. */
	public static int tint(String colorName, float timeSeconds) {
		Integer rgb = COLORS.get(colorName);
		if (rgb == null || colorName.isEmpty()) {
			return 0xFFFFFFFF;
		}
		if (rgb == -1) {
			return 0xFF000000 | (Color.HSBtoRGB((timeSeconds * 0.25F) % 1F, 0.75F, 1F) & 0xFFFFFF);
		}
		return 0xFF000000 | rgb;
	}

	public static String idForName(Map<String, String> map, String displayName) {
		for (Map.Entry<String, String> e : map.entrySet()) {
			if (e.getValue().equals(displayName)) {
				return e.getKey();
			}
		}
		return "";
	}

	public static String validHead(String id) {
		return HEADS.containsKey(id) ? id : "";
	}

	public static String validWings(String id) {
		return WINGS.containsKey(id) ? id : "";
	}

	public static String validColor(String name) {
		return COLORS.containsKey(name) ? name : "";
	}

	/** Minecraft user names: 1-16 letters, digits or underscores. */
	public static String validName(String name) {
		return name != null && name.matches("[A-Za-z0-9_]{1,16}") ? name : "";
	}

	public static String validMinimeeMode(String id) {
		return MINIMEE_MODES.containsKey(id) ? id : "";
	}

	/** Cape ids: static texture names (e.g. "galaxy") or "anim:galaxy". */
	public static String validCape(String id) {
		if (id == null || id.isEmpty()) {
			return "";
		}
		if (ANIMATED_CAPES.containsKey(id)) {
			return id;
		}
		return id.matches("[a-z]{1,16}") ? id : "";
	}
}
