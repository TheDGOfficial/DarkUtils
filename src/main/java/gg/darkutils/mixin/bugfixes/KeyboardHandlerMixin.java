package gg.darkutils.mixin.bugfixes;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
final class KeyboardHandlerMixin {
    private KeyboardHandlerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private final void darkutils$onCharTyped$fixCtrlAndAltOnWaylandIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.preferWayland && DarkUtilsConfig.INSTANCE.fixCtrlAndAltOnWayland && DarkUtils.shouldPreferWayland() && DarkUtils.isWindowPlatformWayland()) {
            final var handle = Minecraft.getInstance().getWindow().handle();
            if (GLFW.GLFW_PRESS == GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) || GLFW.GLFW_PRESS == GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) || GLFW.GLFW_PRESS == GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT)) {
                ci.cancel();
            }
        }
    }
}

