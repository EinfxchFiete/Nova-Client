import org.lwjgl.sdl.*;
public class SdlGlTest { public static void main(String[] a){
 System.out.println("init "+SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO)+" "+SDLError.SDL_GetError());
 SDLVideo.SDL_GL_SetAttribute(17,3);SDLVideo.SDL_GL_SetAttribute(18,3);SDLVideo.SDL_GL_SetAttribute(20,1);SDLVideo.SDL_GL_SetAttribute(19,2);SDLVideo.SDL_GL_SetAttribute(22,1);
 long w=SDLVideo.SDL_CreateWindow("t",640,480,SDLVideo.SDL_WINDOW_OPENGL);
 System.out.println("win "+w+" "+SDLError.SDL_GetError());
 if(w!=0){ long c=SDLVideo.SDL_GL_CreateContext(w); System.out.println("ctx "+c+" "+SDLError.SDL_GetError()); }
}}
