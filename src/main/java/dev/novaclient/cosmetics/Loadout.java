package dev.novaclient.cosmetics;

import com.google.gson.JsonObject;

/**
 * What a player wears. All values are short ids (see {@link CosmeticRegistry}); "" means nothing.
 * The same record is used for the local player and for other Nova players received from the network.
 */
public record Loadout(String cape, String head, String headColor, String wings, String wingsColor) {
	public static final Loadout NONE = new Loadout("", "", "", "", "");

	public JsonObject toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("cape", cape);
		o.addProperty("head", head);
		o.addProperty("headColor", headColor);
		o.addProperty("wings", wings);
		o.addProperty("wingsColor", wingsColor);
		return o;
	}

	public static Loadout fromJson(JsonObject o) {
		if (o == null) {
			return NONE;
		}
		return new Loadout(
			CosmeticRegistry.validCape(str(o, "cape")),
			CosmeticRegistry.validHead(str(o, "head")),
			CosmeticRegistry.validColor(str(o, "headColor")),
			CosmeticRegistry.validWings(str(o, "wings")),
			CosmeticRegistry.validColor(str(o, "wingsColor")));
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
	}
}
