package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    @Unique
    @Nullable
    private String platform;

    private WindowMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Unique
    private final boolean isWayland() {
        if (null == this.platform) {
            this.platform = Window.getGlfwPlatform();
        }

        return "wayland".equals(this.platform);
    }

    @Inject(method = "onFramebufferSizeChanged", at = @At("RETURN"))
    private final void darkutils$fixGuiScaleIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.fixGuiScaleAfterFullscreen) {
            final var wayland = this.isWayland();

            if (!wayland && 480 > this.framebufferHeight) {
                this.fixFramebufferHeight(true);
            } else if (wayland && !this.fullscreen && 480 < this.framebufferHeight) {
                this.fixFramebufferHeight(false);
            }
        }
    }

    @Unique
    private final void fixFramebufferHeight(final boolean setPosSupportedOnPlatform) {
        this.framebufferHeight = 480;

        GLFW.glfwSetWindowSize(this.handle, this.framebufferWidth, this.framebufferHeight);

        if (setPosSupportedOnPlatform) {
            GLFW.glfwSetWindowPos(this.handle, this.x, this.y);
        }

        MinecraftClient.getInstance().onResolutionChanged();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false), remap = false)
    private final void darkutils$modifyOpenGLVersion(final int hint, final int value) {
        GLFW.glfwWindowHint(hint, GLFW.GLFW_CONTEXT_VERSION_MAJOR == hint ? OpenGLVersionOverride.getGLMajorVersion(value) : GLFW.GLFW_CONTEXT_VERSION_MINOR == hint ? OpenGLVersionOverride.getGLMinorVersion(value) : value);
    }
}
