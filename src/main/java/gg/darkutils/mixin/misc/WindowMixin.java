package gg.darkutils.mixin.misc;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.bugfixes.WaylandGameIconFix;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
import gg.darkutils.feat.bugfixes.WaylandGameIconFix;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(Window.class)
final class WindowMixin {
    @Shadow
    private boolean fullscreen;
    @Shadow
    private int framebufferWidth;
    @Shadow
    private int framebufferHeight;
    @Shadow
    @Final
    private long handle;
    @Shadow
    private int x;
    @Shadow
    private int y;

    private WindowMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "onFramebufferResize", at = @At("RETURN"))
    private final void darkutils$fixGuiScaleIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.fixGuiScaleAfterFullscreen) {
            final var wayland = DarkUtils.isWindowPlatformWayland();

            if (!wayland && 480 > this.framebufferHeight) {
                this.darkutils$fixFramebufferHeight(true);
            } else if (wayland && !this.fullscreen && 480 < this.framebufferHeight) {
                this.darkutils$fixFramebufferHeight(false);
            }
        }
    }

    @Unique
    private final void darkutils$fixFramebufferHeight(final boolean setPosSupportedOnPlatform) {
        this.framebufferHeight = 480;

        GLFW.glfwSetWindowSize(this.handle, this.framebufferWidth, this.framebufferHeight);

        if (setPosSupportedOnPlatform) {
            GLFW.glfwSetWindowPos(this.handle, this.x, this.y);
        }

        Minecraft.getInstance().resizeGui();
    }

    @Inject(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuBackend;setWindowHints()V", shift = At.Shift.AFTER))
    private static final void darkutils$afterDefaultWindowHints$fixGameIconOnWaylandIfEnabled(final int width, final int height, @NotNull final String title, final long monitor, @NotNull final GpuBackend backend, @NotNull final CallbackInfoReturnable<Long> cir) {
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

    @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
    private final void darkutils$onSetGameIcon$fixGameIconOnWaylandIfEnabled(@NotNull final PackResources packResources, @NotNull final IconSet iconSet, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.preferWayland && DarkUtilsConfig.INSTANCE.fixGameIconOnWayland && DarkUtils.shouldPreferWayland() && DarkUtils.isWindowPlatformWayland() && !WaylandGameIconFix.desktopFileFailure) {
            try {
                WaylandGameIconFix.setIcon(iconSet.getStandardIcons(packResources));
            } catch (final IOException ioe) {
                DarkUtils.error("@fileName@", "Error getting standard icons from pack resources for use with wayland icon fix, fall backing to vanilla logic", ioe);
                return;
            }

            ci.cancel();
        }
    }
}
