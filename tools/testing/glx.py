import ctypes, os
x=ctypes.CDLL('libX11.so.6'); gl=ctypes.CDLL('libGL.so.1')
x.XOpenDisplay.restype=ctypes.c_void_p
d=x.XOpenDisplay(None); print('display',bool(d))
gl.glXQueryExtension.argtypes=[ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p]
print('glx ext', gl.glXQueryExtension(ctypes.c_void_p(d),None,None))
attrs=(ctypes.c_int*5)(4,8,5,0,0)  # GLX_RGBA, GLX_RED_SIZE? simplified
gl.glXChooseVisual.restype=ctypes.c_void_p
gl.glXChooseVisual.argtypes=[ctypes.c_void_p,ctypes.c_int,ctypes.c_void_p]
a=(ctypes.c_int*3)(4,5,0) # GLX_RGBA, GLX_DOUBLEBUFFER
print('visual', gl.glXChooseVisual(ctypes.c_void_p(d),0,a))
