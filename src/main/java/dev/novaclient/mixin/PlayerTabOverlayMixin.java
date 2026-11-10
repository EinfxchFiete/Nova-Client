package dev.novaclient.mixin;

import dev.novaclient.cosmetics.Badges;
import dev.novaclient.module.utility.TabPingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void novaclient$tabBadges(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		Minecraft mc = Minecraft.getInstance();
		boolean local = mc.player != null && info.getProfile().id().equals(mc.player.getUUID());
		cir.setReturnValue(Badges.decorate(info.getProfile().id(), local, cir.getReturnValue(), false));
	}

	/** Draws the own name icon over the spaces in front of the local player's name. */
	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void novaclient$tabNameIcon(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Component text, int x, int y, int color,
		com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
		original.call(g, font, text, x, y, color);
		if (text != null && text == Badges.lastTabName) {
			dev.novaclient.cosmetics.NameIcons.Icon icon = dev.novaclient.module.cosmetics.NametagModule.activeIcon();
			if (icon != null) {
				int frame = dev.novaclient.module.cosmetics.NametagModule.iconFrame(icon);
				int size = dev.novaclient.cosmetics.NameIcons.SIZE;
				g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon.texture(), x, y - 1, 0, frame * icon.frameH(),
					icon.width(), size, icon.frameW(), icon.frameH(), icon.frameW(), icon.frameH() * icon.frames());
			}
		}
	}

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 13))
	private int novaclient$widerPingColumn(int original) {
		return TabPingModule.active() ? TabPingModule.SLOT_EXTRA : original;
	}

	@Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
	private void novaclient$pingNumber(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo ci) {
		if (TabPingModule.active()) {
			TabPingModule.draw(graphics, slotWidth, xo, yo, info);
			ci.cancel();
		}
	}
}
