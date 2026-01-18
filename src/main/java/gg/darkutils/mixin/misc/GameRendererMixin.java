package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.RenderWorldEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
final class GameRendererMixin {
    private GameRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$overrideNightVisionIfEnabled(@NotNull final LivingEntity entity, final float tickProgress, @NotNull final CallbackInfoReturnable<Float> cir) {
        if (DarkUtilsConfig.INSTANCE.nightVision) {
            cir.setReturnValue(0.9F);
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "CONSTANT", args = "stringValue=hand", shift = At.Shift.BEFORE))
    private final void darkutils$onRenderWorld(@NotNull final RenderTickCounter renderTickCounter, @NotNull final CallbackInfo ci) {
        RenderWorldEvent.INSTANCE.trigger();
    }
}
