package dev.novaclient.mixin;

import dev.novaclient.util.ItemPhysicsState;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements ItemPhysicsState {
	@Unique
	private boolean novaclient$ground;

	@Override
	public boolean novaclient$onGround() {
		return novaclient$ground;
	}

	@Override
	public void novaclient$setOnGround(boolean onGround) {
		novaclient$ground = onGround;
	}
}
