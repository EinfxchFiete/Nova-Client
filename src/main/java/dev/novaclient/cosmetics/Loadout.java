package dev.novaclient.cosmetics;

import com.google.gson.JsonObject;

/**
 * What a player wears. All values are short ids (see {@link CosmeticRegistry}); "" means nothing.
 * The same record is used for the local player and for other Nova players received from the network.
 * {@code minimee} is a Minecraft name whose skin is shown as a mini player at {@code minimeeMode}.
 */
public record Loadout(String cape, String head, String headColor, String wings, String wingsColor, String minimee, String minimeeMode) {
	public static final Loadout NONE = new Loadout("", "", "", "", "", "", "");

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
		if (hasMinimee()) {
			JsonObject m = new JsonObject();
			m.addProperty("name", minimee);
			m.addProperty("mode", minimeeMode);
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
		if (o.has("minimee") && o.get("minimee").isJsonObject()) {
			JsonObject m = o.getAsJsonObject("minimee");
			name = CosmeticRegistry.validName(str(m, "name"));
			mode = CosmeticRegistry.validMinimeeMode(str(m, "mode"));
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
			name, mode);
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
	}
}
