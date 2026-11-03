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
