package dev.novaclient.test.mixin;

import com.mojang.renderpearl.backend.opengl.GlBackend;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TEST ONLY: the virtual X server used for automated tests has no sRGB-capable
 * framebuffer, so drop that request. Never shipped in the release jar.
 */
@Mixin(GlBackend.class)
public abstract class HeadlessGlBackendMixin {
	@Inject(method = "createWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J"))
	private void novaclientTest$noSrgb(String title, int width, int height, long flags, CallbackInfoReturnable<Long> cir) {
		SDLVideo.SDL_GL_SetAttribute(22, 0);
	}
}
