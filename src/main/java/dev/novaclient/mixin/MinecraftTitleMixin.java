package dev.novaclient.mixin;

import dev.novaclient.module.utility.WindowTitleModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftTitleMixin {
	@Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
	private void novaclient$title(CallbackInfoReturnable<String> cir) {
		String t = WindowTitleModule.title((Minecraft) (Object) this);
		if (t != null) {
			cir.setReturnValue(t);
		}
	}
}
