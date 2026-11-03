package dev.novaclient.mixin;

import dev.novaclient.util.PingTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handlePongResponse", at = @At("HEAD"))
	private void novaclient$measurePing(ClientboundPongResponsePacket packet, CallbackInfo ci) {
		PingTracker.onPong(packet.time());
	}
}
