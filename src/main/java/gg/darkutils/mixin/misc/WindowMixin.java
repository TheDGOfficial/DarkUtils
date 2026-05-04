package gg.darkutils.mixin.misc;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.bugfixes.WaylandGameIconFix;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
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
