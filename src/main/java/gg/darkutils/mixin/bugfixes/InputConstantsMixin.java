package gg.darkutils.mixin.bugfixes;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.platform.InputConstants;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InputConstants.class)
final class InputConstantsMixin {
    @Unique
    private static final String WARP_MOUSE = "Lorg/lwjgl/sdl/SDLMouse;SDL_WarpMouseInWindow(JFF)V";

    private InputConstantsMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @WrapWithCondition(method = "grabMouse", at = @At(value = "INVOKE", target = WARP_MOUSE, remap = false))
    private static final boolean darkutils$warpOnGrab(final long window, final float x, final float y) {
        return !DarkUtilsConfig.INSTANCE.neverResetCursorPosition && InputConstantsMixin.darkutils$allowWaylandWarp();
    }

    @WrapWithCondition(method = "releaseMouse", at = @At(value = "INVOKE", target = WARP_MOUSE, remap = false))
    private static final boolean darkutils$warpOnRelease(final long window, final float x, final float y) {
        return InputConstantsMixin.darkutils$allowWaylandWarp();
    }

    @Unique
    private static final boolean darkutils$allowWaylandWarp() {
        return !DarkUtilsConfig.INSTANCE.cursorPosWaylandGLErrorFix || !DarkUtils.isWindowPlatformWayland();
    }
}

