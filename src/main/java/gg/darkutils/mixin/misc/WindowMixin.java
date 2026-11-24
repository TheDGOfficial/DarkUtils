package gg.darkutils.mixin.misc;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.bugfixes.WaylandGameIconFix;
import net.minecraft.server.packs.PackMetadataResources;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    private int width;
    @Shadow
    private int height;
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
    private final void darkutils$fixGuiScaleIfEnabled(final int framebufferWidth, final int framebufferHeight, @NotNull final CallbackInfo ci) {
        if (!DarkUtilsConfig.INSTANCE.fixGuiScaleAfterFullscreen || this.fullscreen || 0 >= framebufferWidth || 0 >= framebufferHeight) {
            return;
        }
        final var wayland = DarkUtils.isWindowPlatformWayland();
        if (wayland ? 480 < framebufferHeight : 480 > framebufferHeight) {
            // SDL takes window coordinates. The next pixel-size event updates the framebuffer and GUI.
            final var targetHeight = (int) Math.ceil(480.0D * this.height / framebufferHeight);
            if (targetHeight != this.height && SDLVideo.SDL_SetWindowSize(this.handle, this.width, targetHeight) && !wayland) {
                SDLVideo.SDL_SetWindowPosition(this.handle, this.x, this.y);
            }
        }
    }

    @Inject(method = "createWindow", at = @At("HEAD"))
    private final void darkutils$setWaylandAppIdIfEnabled(@NotNull final CallbackInfoReturnable<Long> cir) {
        if (DarkUtilsConfig.INSTANCE.fixGameIconOnWayland && DarkUtils.isWindowPlatformWayland()) {
            try {
                WaylandGameIconFix.generateDesktopFile();
            } catch (final IOException ioe) {
                WaylandGameIconFix.desktopFileFailure = true;
                DarkUtils.error("@fileName@", "Error generating desktop file to filesystem for use with wayland icon fix, fall backing to vanilla logic", ioe);

                return;
            }

            if (!SDLInit.SDL_SetAppMetadataProperty(SDLInit.SDL_PROP_APP_METADATA_IDENTIFIER_STRING, WaylandGameIconFix.WAYLAND_APP_ID)) {
                WaylandGameIconFix.desktopFileFailure = true;
                DarkUtils.warn("@fileName@", "Could not set the Wayland application ID: " + SDLError.SDL_GetError());
            }
        }
    }

    @Inject(method = "setIcon(Lnet/minecraft/server/packs/PackMetadataResources;Lcom/mojang/blaze3d/platform/IconSet;)V", at = @At("HEAD"), cancellable = true)
    private final void darkutils$onSetGameIcon$fixGameIconOnWaylandIfEnabled(@NotNull final PackMetadataResources packResources, @NotNull final IconSet iconSet, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.fixGameIconOnWayland && DarkUtils.isWindowPlatformWayland() && !WaylandGameIconFix.desktopFileFailure) {
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
