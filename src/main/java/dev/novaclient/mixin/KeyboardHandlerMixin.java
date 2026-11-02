package dev.novaclient.mixin;

import dev.novaclient.NovaClient;
import dev.novaclient.util.KeyState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	/** action: 1 = press, 0 = release, -1 = repeat. */
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void novaclient$keys(long handle, int action, KeyEvent event, CallbackInfo ci) {
		if (handle != minecraft.getWindow().handle()) {
			return;
		}
		if (action == 1) {
			KeyState.key(event.key(), true);
		} else if (action == 0) {
			KeyState.key(event.key(), false);
		}
		if (action == 1 && minecraft.gui.screen() == null && minecraft.player != null && NovaClient.modules() != null) {
			NovaClient.modules().onKeyPressed(event.key());
		}
	}
}
