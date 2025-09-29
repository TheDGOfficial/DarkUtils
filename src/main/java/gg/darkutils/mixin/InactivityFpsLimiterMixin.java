package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.option.InactivityFpsLimiter;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InactivityFpsLimiter.class)
final class InactivityFpsLimiterMixin {
    @Shadow
    private long lastInputTime;

    private InactivityFpsLimiterMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "getLimitReason", at = @At("HEAD"), cancellable = true)
    private final void darkutils$fixInactivityFpsLimiterIfEnabled(@NotNull final CallbackInfoReturnable<InactivityFpsLimiter.LimitReason> cir) {
        if (DarkUtilsConfig.INSTANCE.fixInactivityFpsLimiter && 0L == this.lastInputTime) {
            this.lastInputTime = Util.getMeasuringTimeMs();
        }
    }

    @Inject(method = "getLimitReason", at = @At("RETURN"), cancellable = true)
    private final void darkutils$removeMainMenuFrameLimitIfEnabled(@NotNull final CallbackInfoReturnable<InactivityFpsLimiter.LimitReason> cir) {
        if (DarkUtilsConfig.INSTANCE.removeMainMenuFrameLimit && InactivityFpsLimiter.LimitReason.OUT_OF_LEVEL_MENU == cir.getReturnValue()) {
            cir.setReturnValue(InactivityFpsLimiter.LimitReason.NONE);
        }
    }
}
