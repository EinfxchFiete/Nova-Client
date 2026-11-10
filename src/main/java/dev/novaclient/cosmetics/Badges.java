package dev.novaclient.cosmetics;

import dev.novaclient.NovaClient;
import dev.novaclient.module.cosmetics.NametagModule;
import dev.novaclient.module.utility.NetworkModule;
import dev.novaclient.network.NovaNetwork;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;

/** Small icons in front of player names: crown for the client owners, star for Nova players. */
public final class Badges {
	/** Owners of Nova Client (Minecraft account UUIDs, names can change). */
	public static final Set<UUID> OWNERS = Set.of(
		UUID.fromString("0c68d1f1-ba5c-4356-833b-5f1cb4e4530c"), // Flipp1009
		UUID.fromString("38f96e1e-970c-4c9f-bb7d-a15c4b07bfcb")  // EinfxchFiete
	);

	private static final Identifier GUI_ATLAS = Identifier.withDefaultNamespace("gui");
	private static final Identifier OWNER = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "badge/owner");
	private static final Identifier NOVA = Identifier.fromNamespaceAndPath(NovaClient.MOD_ID, "badge/nova");

	private Badges() {
	}

	public static boolean isOwner(UUID uuid) {
		return uuid != null && OWNERS.contains(uuid);
	}

	public static MutableComponent ownerIcon() {
		return Component.object(new AtlasSprite(GUI_ATLAS, OWNER), Component.literal("♛"));
	}

	public static MutableComponent novaIcon() {
		return Component.object(new AtlasSprite(GUI_ATLAS, NOVA), Component.literal("✦"));
	}

	/**
	 * Name with badge. local = the player of this client (always a Nova player).
	 * The local player's name is also coloured by the Nametags module.
	 */
	public static Component decorate(UUID uuid, boolean local, Component name, boolean forNametag) {
		if (name == null) {
			return null;
		}
		Component shown = local && (forNametag || NametagModule.tabOn()) ? NametagModule.colorize(name) : name;
		if (!local && dev.novaclient.module.utility.FriendsModule.marking() && dev.novaclient.friends.Friends.isFriend(uuid)) {
			// friends get a green heart in front of the name
			shown = Component.empty().append(Component.literal("❤ ").withColor(0x55FF55)).append(shown);
		}
		MutableComponent icon = null;
		if (isOwner(uuid)) {
			icon = ownerIcon();
		} else if (local ? NametagModule.symbolOn() : novaBadgeVisible(uuid)) {
			icon = novaIcon();
		}
		if (icon == null) {
			return shown;
		}
		return Component.empty().append(icon).append(Component.literal(" ")).append(shown);
	}

	private static boolean novaBadgeVisible(UUID uuid) {
		NetworkModule net = NetworkModule.get();
		return net != null && net.showBadges.isOn() && NovaNetwork.isNovaUser(uuid);
	}
}
