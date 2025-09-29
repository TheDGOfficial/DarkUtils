package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
final class InGameHudMixin {
    private InGameHudMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$skipRenderingArmorProtectionPointsHudIfEnabled(@NotNull final DrawContext context, @NotNull final PlayerEntity player, final int i, final int j, final int k, final int x, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideArmorAndFood) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
    private final void darkutils$skipRenderingEffectsHudIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideEffectsHud) {
            // cancel rendering effects in top right corner of the screen all the time
            ci.cancel();
        }
    }

    @Redirect(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private final void darkutils$transparentScoreboardIfEnabled(final DrawContext context, final int x1, final int y1, final int x2, final int y2, final int color) {
        // draw a transparent background for scoreboard (sidebar) in the right middle of screen, making it transparent, which both looks better and has better performance due to no shadow rendering.
        context.fill(x1, y1, x2, y2, DarkUtilsConfig.INSTANCE.transparentScoreboard ? 0x0000_0000 : color);
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderFoodIfEnabled(@NotNull final DrawContext context, @NotNull final PlayerEntity player, final int top, final int right, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideArmorAndFood) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderMountHealthIfEnabled(@NotNull final DrawContext context, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideMountHealth) {
            ci.cancel();
        }
    }
}
