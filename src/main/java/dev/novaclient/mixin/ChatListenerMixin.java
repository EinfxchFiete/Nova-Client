package dev.novaclient.mixin;

import com.mojang.authlib.GameProfile;
import dev.novaclient.cosmetics.Badges;
import dev.novaclient.module.utility.NetworkModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Owner/Nova badge in front of the sender name of player chat messages. Only works for real (signed) player chat;
 * servers that send chat as system messages (many plugin servers) format the name themselves.
 */
@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
	@ModifyVariable(method = "handlePlayerChatMessage", at = @At("HEAD"), argsOnly = true)
	private ChatType.Bound novaclient$chatBadge(ChatType.Bound bound, PlayerChatMessage message, GameProfile sender) {
		NetworkModule net = NetworkModule.get();
		if (net == null || !net.chatBadges.isOn() || sender == null || bound == null) {
			return bound;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean local = mc.player != null && sender.id().equals(mc.player.getUUID());
		Component name = Badges.decorate(sender.id(), local, bound.name(), false);
		return name == bound.name() ? bound : new ChatType.Bound(bound.chatType(), name, bound.targetName());
	}
}
