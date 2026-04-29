package gg.darkutils.mixin.bugfixes;

import com.mojang.blaze3d.platform.InputConstants;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InputConstants.class)
final class InputConstantsMixin {
    private InputConstantsMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "grabOrReleaseMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"))
    private static final void darkutils$glfwSetCursorPos(final long window, final double xpos, final double ypos) {
        if (!DarkUtilsConfig.INSTANCE.cursorPosWaylandGLErrorFix || !DarkUtils.isWindowPlatformWayland()) {
            GLFW.glfwSetCursorPos(window, xpos, ypos);
        }
    }
}

