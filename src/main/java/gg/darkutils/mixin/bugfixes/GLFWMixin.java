package gg.darkutils.mixin.bugfixes;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GLFW.class, priority = 1_001)
final class GLFWMixin {
    private GLFWMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "glfwSetCursorPos", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$onGlfwSetCursorPos(final long window, final double xpos, final double ypos, @NotNull final CallbackInfo ci) {
        final var config = DarkUtilsConfig.INSTANCE;

        if (config.cursorPosWaylandGLErrorFix && DarkUtils.isWindowPlatformWayland()) {
            final var stack = config.debugMode;

            DarkUtils.warn("@fileName@", "Cancelling setting cursor position in Wayland as it is not supported." + (stack ? "" : " To log which code path tried to set the cursor position, enable Debug Mode feature in Development category of the config."));
            if (stack) {
                Thread.dumpStack();
            }

            ci.cancel();
        }
    }
}

