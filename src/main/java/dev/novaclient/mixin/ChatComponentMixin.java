package dev.novaclient.mixin;

import dev.novaclient.module.utility.ChatTimestampsModule;
import dev.novaclient.module.utility.CompactChatModule;
import dev.novaclient.module.utility.LongChatModule;
import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@Shadow
	@Final
	private List<GuiMessage> allMessages;

	@Shadow
	private void refreshTrimmedMessages() {
		throw new AssertionError();
	}

	@Unique
	private String novaclient$lastText;
	@Unique
	private GuiMessage novaclient$lastMessage;
	@Unique
	private int novaclient$count = 1;
	@Unique
	private boolean novaclient$merged;

	/** Timestamps and merging of repeated messages ("(x2)"). */
	@ModifyVariable(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;"
			+ "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("HEAD"),
		argsOnly = true
	)
	private Component novaclient$modify(Component message) {
		novaclient$merged = false;
		if (dev.novaclient.module.utility.ChatFilterModule.hides(message)) {
			// filtered: dropped by novaclient$filter, do not touch the merge state
			return message;
		}
		Component result = message;
		if (CompactChatModule.active()) {
			String text = message.getString();
			if (text.equals(novaclient$lastText) && !allMessages.isEmpty() && allMessages.get(0) == novaclient$lastMessage) {
				novaclient$count++;
				allMessages.remove(0);
				novaclient$merged = true;
				result = message.copy().append(Component.literal(" (x" + novaclient$count + ")").withColor(0x9A9AAE));
			} else {
				novaclient$count = 1;
			}
			novaclient$lastText = text;
		}
		return ChatTimestampsModule.apply(result);
	}

	/** Chat filter: drop the message before it is added. */
	@Inject(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;"
			+ "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void novaclient$filter(Component message, net.minecraft.network.chat.MessageSignature signature,
		net.minecraft.client.multiplayer.chat.GuiMessageSource source, net.minecraft.client.multiplayer.chat.GuiMessageTag tag, CallbackInfo ci) {
		if (dev.novaclient.module.utility.ChatFilterModule.hides(message)) {
			ci.cancel();
		}
	}

	@Inject(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;"
			+ "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("TAIL")
	)
	private void novaclient$afterAdd(CallbackInfo ci) {
		novaclient$lastMessage = allMessages.isEmpty() ? null : allMessages.get(0);
		if (novaclient$merged) {
			// the old line is still in the display list: rebuild it from allMessages
			novaclient$merged = false;
			refreshTrimmedMessages();
		}
	}

	@ModifyConstant(method = {"addMessageToQueue", "addMessageToDisplayQueue"}, constant = @Constant(intValue = 100))
	private int novaclient$history(int vanilla) {
		return LongChatModule.limit(vanilla);
	}
}
