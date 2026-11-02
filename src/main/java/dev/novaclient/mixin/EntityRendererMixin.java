package dev.novaclient.mixin;

import dev.novaclient.module.cosmetics.NametagModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	/** Nova symbol in front of your own name. */
	@Inject(method = "getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
	private void novaclient$nameSymbol(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (entity == Minecraft.getInstance().player && cir.getReturnValue() != null) {
			cir.setReturnValue(NametagModule.decorate(cir.getReturnValue()));
		}
	}
}
