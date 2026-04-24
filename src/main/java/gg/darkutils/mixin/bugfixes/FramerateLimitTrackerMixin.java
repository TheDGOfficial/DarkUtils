package gg.darkutils.mixin.bugfixes;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
final class FramerateLimitTrackerMixin {
    @Shadow
    private long latestInputTime;
    @Unique
    private boolean darkutils$uninitialized;

    private FramerateLimitTrackerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private final void darkutils$init(@NotNull final CallbackInfo ci) {
        this.darkutils$uninitialized = true;
    }

    @Inject(method = "getThrottleReason", at = @At("HEAD"), cancellable = true)
    private final void darkutils$fixInactivityFpsLimiterIfEnabled(@NotNull final CallbackInfoReturnable<FramerateLimitTracker.FramerateThrottleReason> cir) {
        if (DarkUtilsConfig.INSTANCE.fixInactivityFpsLimiter && this.darkutils$uninitialized && 0L == this.latestInputTime) {
            this.darkutils$uninitialized = false;
            this.latestInputTime = Util.getMillis();
        }
    }

    @Inject(method = "getThrottleReason", at = @At("RETURN"), cancellable = true)
    private final void darkutils$removeMainMenuFrameLimitIfEnabled(@NotNull final CallbackInfoReturnable<FramerateLimitTracker.FramerateThrottleReason> cir) {
        if (DarkUtilsConfig.INSTANCE.removeMainMenuFrameLimit && FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU == cir.getReturnValue()) {
            cir.setReturnValue(FramerateLimitTracker.FramerateThrottleReason.NONE);
        }
    }
}
