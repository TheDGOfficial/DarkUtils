package gg.darkutils.mixin.bugfixes;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.sdl.SDLMouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SDLMouse.class, priority = 1_001, remap = false)
final class SDLMouseMixin {
    private SDLMouseMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "SDL_WarpMouseInWindow(JFF)V", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$onWarpMouseInWindow(final long window, final float x, final float y, @NotNull final CallbackInfo ci) {
        final var config = DarkUtilsConfig.INSTANCE;

        if (config.cursorPosWaylandGLErrorFix && DarkUtils.isWindowPlatformWayland()) {
            final var stack = config.debugMode;

            DarkUtils.warn("@fileName@", "Skipping an SDL cursor warp on Wayland." + (stack ? "" : " Enable Debug Mode to see which code path requested it."));
            if (stack) {
                Thread.dumpStack();
            }

            ci.cancel();
        }
    }
}
