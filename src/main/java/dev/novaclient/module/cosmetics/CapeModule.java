package dev.novaclient.module.cosmetics;

import dev.novaclient.NovaClient;
import dev.novaclient.cosmetics.CosmeticRegistry;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/** Cape cosmetic: 14 static designs, 6 animated designs or a cycling mode. Other Nova players see it too. */
public class CapeModule extends Module {
	private static CapeModule instance;

	public static final String[] DESIGNS = {"Nova", "Galaxie", "Feuer", "Ozean", "Wald", "Gold", "Eis", "Regenbogen", "Kirschblüte",
		"Schachbrett", "Lava", "Nacht", "Sonnenuntergang", "Matrix"};

	public final ModeSetting design = add(new ModeSetting("Design", "Nova", modes()));
	public final BoolSetting elytra = add(new BoolSetting("Auch auf Elytren", true));

	public CapeModule() {
		super("Cape", "Eigenes Cape: 14 Designs, 6 animierte oder wechselnd", Category.COSMETICS, false);
		instance = this;
	}

	public static CapeModule get() {
		return instance;
	}

	private static String[] modes() {
		List<String> out = new ArrayList<>(List.of(DESIGNS));
		out.addAll(CosmeticRegistry.ANIMATED_CAPES.values());
		out.add("Wechselnd");
		return out.toArray(new String[0]);
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Texture name of a static design, e.g. "Galaxie" -> "galaxy". */
	public static String textureName(String design) {
		return switch (design) {
			case "Kirschblüte" -> "cherry";
			case "Galaxie" -> "galaxy";
			case "Feuer" -> "fire";
			case "Ozean" -> "ocean";
			case "Wald" -> "forest";
			case "Eis" -> "ice";
			case "Regenbogen" -> "rainbow";
			case "Schachbrett" -> "checker";
			case "Nacht" -> "night";
			case "Sonnenuntergang" -> "sunset";
			default -> design.toLowerCase(Locale.ROOT);
		};
	}

	/** Network id of a design name: "galaxy", "anim:galaxy". */
	public static String idFor(String design) {
		String anim = CosmeticRegistry.idForName(CosmeticRegistry.ANIMATED_CAPES, design);
		if (!anim.isEmpty()) {
			return anim;
		}
		if (design.equals("Wechselnd")) {
			return textureName(DESIGNS[(int) ((System.currentTimeMillis() / 2000L) % DESIGNS.length)]);
		}
		return textureName(design);
	}

	public String currentId() {
		return idFor(design.get());
	}

	/** Texture for a cape id; animated capes pick the current frame (10 frames per second). */
	public static Identifier texture(String id) {
		if (id.startsWith("anim:")) {
			int frame = (int) ((System.currentTimeMillis() / 100L) % CosmeticRegistry.CAPE_FRAMES);
			return Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "cape/anim/" + id.substring(5) + "_" + frame);
		}
		return Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "cape/" + id);
	}

	/** Full texture file path of a cape id (for drawing it in menus). */
	public static Identifier texturePath(String id) {
		Identifier t = texture(id);
		return t.withPath(p -> "textures/" + p + ".png");
	}

	/** Kept for older callers/tests: id of a static design as texture id. */
	public static Identifier textureId(String design) {
		return texture(idFor(design));
	}

	/** Returns the skin with the given cape id (and elytra), or the skin unchanged when id is empty. */
	public static PlayerSkin withCape(PlayerSkin skin, String id, boolean onElytra) {
		if (id == null || id.isEmpty()) {
			return skin;
		}
		ClientAsset.ResourceTexture cape = new ClientAsset.ResourceTexture(texture(id));
		return new PlayerSkin(skin.body(), cape, onElytra ? cape : skin.elytra(), skin.model(), skin.secure());
	}

	/** Returns the skin with our own cape, or the original skin. */
	public static PlayerSkin apply(PlayerSkin skin) {
		CapeModule c = instance;
		if (c == null || !c.isEnabled()) {
			return skin;
		}
		return withCape(skin, c.currentId(), c.elytra.isOn());
	}
}
