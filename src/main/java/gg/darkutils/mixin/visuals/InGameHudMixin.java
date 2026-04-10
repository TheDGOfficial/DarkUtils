package gg.darkutils.mixin.visuals;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
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

    @Redirect(method = "renderPlayerList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private final boolean darkutils$showTabListIfEnabled(@NotNull final KeyBinding keyBinding) {
        if (MinecraftClient.getInstance().options.playerListKey != keyBinding) {
            throw new IllegalStateException("@fileName@ needs updating (" + DarkUtils.class.getSimpleName() + ')');
        }

        return keyBinding.isPressed() || DarkUtilsConfig.INSTANCE.persistentTabListWhileFarming && ActivityState.isActivelyFarming() && LocationUtils.isInGarden();
    }

    @Inject(at = @At("HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
    private final void darkutils$skipRenderingEffectsHudIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideEffectsHud) {
            // cancel rendering effects in top right corner of the screen all the time
            ci.cancel();
        }
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
