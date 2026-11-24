package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "ca.spottedleaf.starlight.common.light.vanillainterface.BaseLevelLightEngineVanillaInterface", remap = false)
final class ScalableLuxLightEngineMixin {
    @Inject(method = "runLightUpdates()I", at = @At("HEAD"), cancellable = true)
    private final void darkutils$stopLightUpdatesIfEnabled(@NotNull final CallbackInfoReturnable<Integer> cir) {
        if (DarkUtilsConfig.INSTANCE.fullbright && (!LocationUtils.isInSingleplayer() || !RenderUtils.isNotCallingFromRenderThread())) {
            cir.setReturnValue(0);
        }
    }
}
