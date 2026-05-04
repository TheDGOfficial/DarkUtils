package gg.darkutils.mixin.misc;

import gg.darkutils.feat.performance.OpenGLVersionOverride;
import com.mojang.blaze3d.opengl.GlBackend;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlBackend.class)
final class GlBackendMixin {
    private GlBackendMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "setWindowHints", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false), remap = false)
    private final void darkutils$modifyOpenGLVersion(final int hint, final int value) {
        GLFW.glfwWindowHint(hint, GLFW.GLFW_CONTEXT_VERSION_MAJOR == hint ? OpenGLVersionOverride.getGLMajorVersion(value) : GLFW.GLFW_CONTEXT_VERSION_MINOR == hint ? OpenGLVersionOverride.getGLMinorVersion(value) : value);
    }
}
