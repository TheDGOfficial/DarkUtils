package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
final class LevelLoadingScreenMixin {
    @Shadow
    private LevelLoadTracker loadTracker;

    private LevelLoadingScreenMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = {"extractRenderState", "extractBackground"}, at = @At("HEAD"), cancellable = true, require = 2)
    private final void darkutils$hideLevelLoadingScreen(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.seamlessHypixelTransfers && LocationUtils.isInHypixel() && this.loadTracker.statusView() == null) {
            ci.cancel();
        }
    }
}
