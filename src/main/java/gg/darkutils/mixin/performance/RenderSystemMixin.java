package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.performance.Clocksource;
import net.minecraft.util.TimeSource;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.sdl.SDLHints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
final class RenderSystemMixin {
    private RenderSystemMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "initBackendSystem", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLInit;SDL_Init(I)Z", remap = false))
    private static final void darkutils$preferWaylandIfEnabled(@NotNull final CallbackInfoReturnable<TimeSource.NanoTimeSource> cir) {
        if (DarkUtilsConfig.INSTANCE.preferWayland && DarkUtils.shouldPreferWayland()
                && null == SDLHints.SDL_GetHint(SDLHints.SDL_HINT_VIDEO_DRIVER)
                && null == SDLHints.SDL_GetHint("SDL_VIDEODRIVER")
                && SDLHints.SDL_SetHint(SDLHints.SDL_HINT_VIDEO_DRIVER, "wayland,x11")) {
            DarkUtils.info("@fileName@", "Requesting the SDL Wayland video driver with X11 as a fallback.");
        }
    }

    @ModifyReturnValue(method = "initBackendSystem", at = @At("RETURN"))
    @NotNull
    private static final TimeSource.NanoTimeSource darkutils$optimizeClocksourceIfEnabled(@NotNull final TimeSource.NanoTimeSource source) {
        return () -> DarkUtilsConfig.INSTANCE.optimizeClocksource ? Clocksource.getNanos() : source.getAsLong();
    }
}
