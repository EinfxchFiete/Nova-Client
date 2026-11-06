package dev.novaclient.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novaclient.NovaClient;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.jspecify.annotations.Nullable;

/**
 * Pet models converted from Blockbench by tools/assets/bbmodel_pet.py (assets/novaclient/models/pets/&lt;name&gt;.json,
 * texture textures/pets/&lt;name&gt;.png). The root part holds the groups of the model by name.
 */
public final class PetModels {
	private static final Map<String, ModelPart> CACHE = new HashMap<>();

	private PetModels() {
	}

	/** The model root, or null if the model could not be loaded. */
	public static synchronized @Nullable ModelPart get(String name) {
		if (CACHE.containsKey(name)) {
			return CACHE.get(name);
		}
		ModelPart part = null;
		try (InputStream in = PetModels.class.getResourceAsStream("/assets/novaclient/models/pets/" + name + ".json")) {
			if (in != null) {
				part = build(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not load pet model {}", name, e);
		}
		CACHE.put(name, part);
		return part;
	}

	private static float f(JsonArray a, int i) {
		return a.get(i).getAsFloat();
	}

	private static ModelPart build(JsonObject json) {
		MeshDefinition mesh = new MeshDefinition();
		Map<String, PartDefinition> defs = new HashMap<>();
		for (JsonElement el : json.getAsJsonArray("parts")) {
			JsonObject p = el.getAsJsonObject();
			String parent = p.get("parent").isJsonNull() ? null : p.get("parent").getAsString();
			PartDefinition into = parent == null ? mesh.getRoot() : defs.getOrDefault(parent, mesh.getRoot());
			CubeListBuilder cubes = CubeListBuilder.create();
			for (JsonElement ce : p.getAsJsonArray("cubes")) {
				JsonObject c = ce.getAsJsonObject();
				JsonArray uv = c.getAsJsonArray("uv");
				JsonArray from = c.getAsJsonArray("from");
				JsonArray size = c.getAsJsonArray("size");
				cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
					.addBox(f(from, 0), f(from, 1), f(from, 2), f(size, 0), f(size, 1), f(size, 2), new CubeDeformation(c.get("inflate").getAsFloat()));
			}
			JsonArray off = p.getAsJsonArray("offset");
			JsonArray rot = p.getAsJsonArray("rotation");
			defs.put(p.get("name").getAsString(), into.addOrReplaceChild(p.get("name").getAsString(), cubes,
				PartPose.offsetAndRotation(f(off, 0), f(off, 1), f(off, 2), f(rot, 0), f(rot, 1), f(rot, 2))));
		}
		JsonArray tex = json.getAsJsonArray("texture_size");
		return LayerDefinition.create(mesh, tex.get(0).getAsInt(), tex.get(1).getAsInt()).bakeRoot();
	}

	/** A group of the model (null if it has none with this name). */
	public static @Nullable ModelPart find(ModelPart root, String name) {
		if (root.hasChild(name)) {
			return root.getChild(name);
		}
		for (ModelPart child : root.getAllParts()) {
			if (child != root && child.hasChild(name)) {
				return child.getChild(name);
			}
		}
		return null;
	}
}
