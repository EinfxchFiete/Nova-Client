package dev.novaclient.mixin;

import dev.novaclient.util.PingTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Shadow
	private ClientLevel level;

	@Inject(method = "handlePongResponse", at = @At("HEAD"))
	private void novaclient$measurePing(ClientboundPongResponsePacket packet, CallbackInfo ci) {
		PingTracker.onPong(packet.time());
	}

	// TAIL: the handlers first hop from the network thread to the main thread, only that second call reaches the end
	@Inject(method = "handleSetTime", at = @At("TAIL"))
	private void novaclient$measureTps(ClientboundSetTimePacket packet, CallbackInfo ci) {
		dev.novaclient.util.TpsTracker.onTime(packet.gameTime());
	}

	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void novaclient$totem(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		if (packet.getEventId() == net.minecraft.world.entity.EntityEvent.PROTECTED_FROM_DEATH && level != null) {
			Entity entity = packet.getEntity(level);
			if (entity != null) {
				dev.novaclient.module.utility.TotemPopsModule.onTotem(entity);
			}
		}
	}
}
