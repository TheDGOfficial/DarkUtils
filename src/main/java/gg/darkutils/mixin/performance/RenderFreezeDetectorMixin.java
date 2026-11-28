package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.performance.RenderFreezeDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
final class RenderFreezeDetectorMixin {
    @Unique
    private volatile @Nullable RenderFreezeDetector darkutils$renderFreezeDetector;

    @Inject(method = "run", at = @At("HEAD"))
    private final void darkutils$watchRenderThread(@NotNull final CallbackInfo ci) {
        final var config = DarkUtilsConfig.INSTANCE;
        final var detector = new RenderFreezeDetector(FabricLoader.getInstance().getGameDir(),
                Math.clamp(config.renderFreezePollIntervalMs, 1, DarkUtilsConfig.MAX_RENDER_FREEZE_POLL_INTERVAL_MS),
                Math.clamp(config.renderFreezeThresholdMs, 1, DarkUtilsConfig.MAX_RENDER_FREEZE_THRESHOLD_MS),
                config.haltOnRenderFreeze);
        this.darkutils$renderFreezeDetector = detector;
        detector.start();
    }

    // The early return for an already acquired surface skips the frame.
    @Inject(method = "renderFrame", at = @At("TAIL"))
    private final void darkutils$recordFrame(final boolean tick, @NotNull final CallbackInfo ci) {
        final var detector = this.darkutils$renderFreezeDetector;
        if (null != detector) {
            detector.frameCompleted();
        }
    }

    @Inject(method = "run", at = @At("RETURN"))
    private final void darkutils$stopAfterRun(@NotNull final CallbackInfo ci) {
        this.darkutils$stopWatchingRenderThread();
    }

    @Inject(method = {"stop", "emergencySaveAndCrash"}, at = @At("HEAD"))
    private final void darkutils$stopBeforeShutdown(@NotNull final CallbackInfo ci) {
        this.darkutils$stopWatchingRenderThread();
    }

    @Unique
    private final void darkutils$stopWatchingRenderThread() {
        final var detector = this.darkutils$renderFreezeDetector;
        if (null != detector) {
            detector.stop();
            this.darkutils$renderFreezeDetector = null;
        }
    }
}
