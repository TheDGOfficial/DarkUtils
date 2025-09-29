package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffectsDisplay.class)
final class StatusEffectsDisplayMixin {
    private StatusEffectsDisplayMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "drawStatusEffects", at = @At("HEAD"), cancellable = true)
    private final void darkutils$cancelDrawIfEnabled(@NotNull final DrawContext context, final int mouseX, final int mouseY, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideEffectsInInventory) {
            ci.cancel();
        }
    }

    @Inject(method = "drawStatusEffectTooltip", at = @At("HEAD"), cancellable = true)
    private final void darkutils$cancelDrawTooltipIfEnabled(@NotNull final DrawContext context, final int mouseX, final int mouseY, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideEffectsInInventory) {
            ci.cancel();
        }
    }
}
