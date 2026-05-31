package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import org.lwjgl.glfw.GLFW;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

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

    @ModifyArgs(method = "createDevice(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;)Lcom/mojang/blaze3d/systems/GpuDevice;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlDevice;<init>(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;)V"))
    private static final void darkutils$disableGlDebugIfEnabled(@NotNull final Args args) {
        if (DarkUtilsConfig.INSTANCE.disableGlDebug) {
            args.set(2, new GpuDebugOptions(0, false, false));
        }
    }
}
