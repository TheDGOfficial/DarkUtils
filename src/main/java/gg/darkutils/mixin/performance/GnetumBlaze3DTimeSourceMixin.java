package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.performance.Clocksource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.decce.gnetum.time.Blaze3DTimeSource", remap = false)
final class GnetumBlaze3DTimeSourceMixin {
    private GnetumBlaze3DTimeSourceMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "nanos", at = @At("HEAD"), cancellable = true)
    private final void darkutils$optimizeClocksourceIfEnabled(@NotNull final CallbackInfoReturnable<Long> cir) {
        if (DarkUtilsConfig.INSTANCE.optimizeClocksource) {
            cir.setReturnValue(Clocksource.getNanos());
        }
    }
}
