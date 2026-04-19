package gg.darkutils.mixin.misc;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
