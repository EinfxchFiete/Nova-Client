package dev.novaclient.cosmetics;

import com.google.gson.JsonObject;

/**
 * What a player wears. All values are short ids (see {@link CosmeticRegistry}); "" means nothing.
 * The same record is used for the local player and for other Nova players received from the network.
 * {@code minimee} is a Minecraft name whose skin is shown as a mini player at {@code minimeeMode}.
 */
public record Loadout(String cape, String head, String headColor, String wings, String wingsColor, String minimee, String minimeeMode,
	String extra, String extraColor, String minimeeAnim, String minimeeSize) {
	public static final Loadout NONE = new Loadout("", "", "", "", "", "", "", "", "", "none", "normal");

	public boolean hasMinimee() {
		return !minimee.isEmpty() && !minimeeMode.isEmpty();
	}

	public JsonObject toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("cape", cape);
		o.addProperty("head", head);
		o.addProperty("headColor", headColor);
		o.addProperty("wings", wings);
		o.addProperty("wingsColor", wingsColor);
		o.addProperty("extra", extra);
		o.addProperty("extraColor", extraColor);
		if (hasMinimee()) {
			JsonObject m = new JsonObject();
			m.addProperty("name", minimee);
			m.addProperty("mode", minimeeMode);
			m.addProperty("anim", minimeeAnim);
			m.addProperty("size", minimeeSize);
			o.add("minimee", m);
		}
		return o;
	}

	public static Loadout fromJson(JsonObject o) {
		if (o == null) {
			return NONE;
		}
		String name = "";
		String mode = "";
		String anim = "none";
		String size = "normal";
		if (o.has("minimee") && o.get("minimee").isJsonObject()) {
			JsonObject m = o.getAsJsonObject("minimee");
			name = CosmeticRegistry.validName(str(m, "name"));
			mode = CosmeticRegistry.validMinimeeMode(str(m, "mode"));
			anim = CosmeticRegistry.validMinimeeAnim(str(m, "anim"));
			size = CosmeticRegistry.validMinimeeSize(str(m, "size"));
			if (name.isEmpty() || mode.isEmpty()) {
				name = "";
				mode = "";
			}
		}
		return new Loadout(
			CosmeticRegistry.validCape(str(o, "cape")),
			CosmeticRegistry.validHead(str(o, "head")),
			CosmeticRegistry.validColor(str(o, "headColor")),
			CosmeticRegistry.validWings(str(o, "wings")),
			CosmeticRegistry.validColor(str(o, "wingsColor")),
			name, mode,
			CosmeticRegistry.validExtra(str(o, "extra")),
			CosmeticRegistry.validColor(str(o, "extraColor")),
			anim, size);
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
	}
}
