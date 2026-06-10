package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.mixinquirks.HolderFields;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.lighting.LevelLightEngine;

import org.jetbrains.annotations.NotNull;

@Mixin(LevelLightEngine.class)
final class LevelLightEngineMixin {
    @Inject(method = "runLightUpdates", at = @At("HEAD"), cancellable = true)
    private final void darkutils$stopLightUpatesIfEnabled(@NotNull final CallbackInfoReturnable<Integer> cir) {
        if (DarkUtilsConfig.INSTANCE.fullbright && !RenderUtils.isNotCallingFromRenderThread() && HolderFields.ScalableLuxValues.HAS_SCALABLE_LUX) {
            cir.setReturnValue(0);
        }
    }
}
