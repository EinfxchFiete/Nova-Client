package dev.novaclient.module.cosmetics;

import dev.novaclient.NovaClient;
import dev.novaclient.module.Category;
import dev.novaclient.module.Module;
import dev.novaclient.setting.BoolSetting;
import dev.novaclient.setting.ModeSetting;
import java.util.Locale;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/** Client-side cape for your own player (only you can see it). */
public class CapeModule extends Module {
	private static CapeModule instance;

	public static final String[] DESIGNS = {"Nova", "Galaxie", "Feuer", "Ozean", "Wald", "Gold", "Eis", "Regenbogen", "Kirschblüte", "Schachbrett"};

	public final ModeSetting design = add(new ModeSetting("Design", "Nova", DESIGNS));
	public final BoolSetting elytra = add(new BoolSetting("Auch auf Elytren", true));

	public CapeModule() {
		super("Cape", "Eigenes Cape mit 10 Designs (nur für dich sichtbar)", Category.COSMETICS, false);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

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
			default -> design.toLowerCase(Locale.ROOT);
		};
	}

	public static Identifier textureId(String design) {
		return Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "cape/" + textureName(design));
	}

	/** Returns the skin with our cape, or the original skin. */
	public static PlayerSkin apply(PlayerSkin skin) {
		CapeModule c = instance;
		if (c == null || !c.isEnabled()) {
			return skin;
		}
		ClientAsset.ResourceTexture cape = new ClientAsset.ResourceTexture(textureId(c.design.get()));
		return new PlayerSkin(skin.body(), cape, c.elytra.isOn() ? cape : skin.elytra(), skin.model(), skin.secure());
	}
}
