package dev.novaclient.mixin;

import dev.novaclient.NovaClient;
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

	/** Module keybinds: action 1 = initial press (0 = release, -1 = repeat). */
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void novaclient$moduleKeybinds(long handle, int action, KeyEvent event, CallbackInfo ci) {
		if (action == 1 && handle == minecraft.getWindow().handle() && minecraft.gui.screen() == null
			&& minecraft.player != null && NovaClient.modules() != null) {
			NovaClient.modules().onKeyPressed(event.key());
		}
	}
}
