package dev.novaclient.util;

/** Extra data on the item render state for item physics. */
public interface ItemPhysicsState {
	boolean novaclient$onGround();

	void novaclient$setOnGround(boolean onGround);
}
