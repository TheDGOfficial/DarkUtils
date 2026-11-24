package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.Blaze3D;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.performance.Clocksource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Blaze3D.class)
final class Blaze3DMixin {
    private Blaze3DMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @WrapOperation(method = "getTime", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLTimer;SDL_GetTicksNS()J", remap = false))
    private static final long darkutils$optimizeClocksourceIfEnabled(@NotNull final Operation<Long> original) {
        return DarkUtilsConfig.INSTANCE.optimizeClocksource ? Clocksource.getNanos() : original.call();
    }
}
