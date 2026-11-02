package dev.novaclient.mixin;

import dev.novaclient.module.visual.ZoomModule;
import dev.novaclient.util.CpsTracker;
import dev.novaclient.util.KeyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "onButton", at = @At("HEAD"))
	private void novaclient$buttons(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
		if (handle != minecraft.getWindow().handle()) {
			return;
		}
		KeyState.mouse(rawButtonInfo.button(), action == 1);
		if (action == 1 && minecraft.gui.screen() == null) {
			CpsTracker.onClick(rawButtonInfo.button());
		}
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void novaclient$zoomScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (handle == minecraft.getWindow().handle() && minecraft.gui.screen() == null && ZoomModule.onScroll(yoffset)) {
			ci.cancel();
		}
	}
}
