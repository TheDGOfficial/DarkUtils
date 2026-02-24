package gg.darkutils.mixin.bugfixes;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;

import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.NotNull;

@Mixin(InputUtil.class)
final class InputUtilMixin {
    @Redirect(method = "setCursorParameters", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"))
    private static final void darkutils$glfwSetCursorPos(final long window, final double xpos, final double ypos) {
        if (!DarkUtilsConfig.INSTANCE.cursorPosWaylandGLErrorFix || !DarkUtils.isWindowPlatformWayland()) {
            GLFW.glfwSetCursorPos(window, xpos, ypos);
        }
    }
}

