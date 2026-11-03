package dev.novaclient.mixin;

import dev.novaclient.cosmetics.Badges;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	/** Owner crown / Nova star in front of player names, own name colour. */
	@Inject(method = "getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
	private void novaclient$badges(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (entity instanceof Player player && cir.getReturnValue() != null) {
			boolean local = player == Minecraft.getInstance().player;
			cir.setReturnValue(Badges.decorate(player.getUUID(), local, cir.getReturnValue(), true));
		}
	}
}
