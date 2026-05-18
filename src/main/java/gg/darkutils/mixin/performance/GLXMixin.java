package gg.darkutils.mixin.performance;

import com.mojang.blaze3d.platform.GLX;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.bugfixes.WaylandGameIconFix;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.LongSupplier;

import java.io.IOException;

@Mixin(GLX.class)
final class GLXMixin {
    private GLXMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "_initGlfw", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwInitHint(II)V"))
    private static final void darkutils$onInitGlfw$onGlfwInitHint$preferWaylandIfEnabled(final int hint, final int value) {
        if (GLFW.GLFW_PLATFORM == hint && GLFW.GLFW_PLATFORM_X11 == value) {
            if (DarkUtilsConfig.INSTANCE.preferWayland && DarkUtils.shouldPreferWayland()) {
                DarkUtils.info("@fileName@", "Requesting GLFW native Wayland backend with glfwInitHint over the default XWayland backend. If your game crashes here, disable the Prefer Wayland option from config manually.");
                GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_WAYLAND);
                return;
            }
        }

        GLFW.glfwInitHint(hint, value);
    }

    @Inject(method = "_initGlfw", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V", shift = At.Shift.AFTER))
    private static final void darkutils$afterDefaultWindowHints$fixGameIconOnWaylandIfEnabled(@NotNull final CallbackInfoReturnable<LongSupplier> ci) {
        if (DarkUtilsConfig.INSTANCE.preferWayland && DarkUtilsConfig.INSTANCE.fixGameIconOnWayland && DarkUtils.shouldPreferWayland() && DarkUtils.isWindowPlatformWayland()) {
            try {
                WaylandGameIconFix.generateDesktopFile();
            } catch (final IOException ioe) {
                WaylandGameIconFix.desktopFileFailure = true;
                DarkUtils.error("@fileName@", "Error generating desktop file to filesystem for use with wayland icon fix, fall backing to vanilla logic", ioe);

                return;
            }

            GLFW.glfwWindowHintString(GLFW.GLFW_WAYLAND_APP_ID, WaylandGameIconFix.WAYLAND_APP_ID);
        }
    }
}

