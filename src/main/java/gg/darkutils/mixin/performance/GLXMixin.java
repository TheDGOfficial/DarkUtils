package gg.darkutils.mixin.performance;

import com.mojang.blaze3d.platform.GLX;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.LongSupplier;

@Mixin(GLX.class)
final class GLXMixin {
    private GLXMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "_initGlfw", at = @At("HEAD"))
    private static final void darkutils$onInitGlfw$preferWaylandIfEnabled(@NotNull final CallbackInfoReturnable<LongSupplier> cir) {
        if (DarkUtilsConfig.INSTANCE.preferWayland && DarkUtils.shouldPreferWayland()) {
            DarkUtils.info("@fileName@", "Requesting GLFW native Wayland backend with glfwInitHint over the default XWayland backend. If your game crashes here, disable the Prefer Wayland option from config manually.");
            GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_WAYLAND);
        }
    }
}

