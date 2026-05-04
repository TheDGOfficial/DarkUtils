package gg.darkutils.mixin.visuals;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
final class GuiMixin {
    private GuiMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$skipRenderingArmorProtectionPointsHudIfEnabled(@NotNull final GuiGraphicsExtractor context, @NotNull final Player player, final int i, final int j, final int k, final int x, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideArmorAndFood) {
            ci.cancel();
        }
    }

    @Redirect(method = "extractTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private final boolean darkutils$showTabListIfEnabled(@NotNull final KeyMapping keyBinding) {
        if (Minecraft.getInstance().options.keyPlayerList != keyBinding) {
            throw new IllegalStateException("@fileName@ needs updating (" + DarkUtils.class.getSimpleName() + ')');
        }

        return keyBinding.isDown() || DarkUtilsConfig.INSTANCE.persistentTabListWhileFarming && ActivityState.isActivelyFarming() && LocationUtils.isInGarden();
    }

    @Inject(at = @At("HEAD"), method = "extractEffects", cancellable = true)
    private final void darkutils$skipRenderingEffectsHudIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideEffectsHud) {
            // cancel rendering effects in top right corner of the screen all the time
            ci.cancel();
        }
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderFoodIfEnabled(@NotNull final GuiGraphicsExtractor context, @NotNull final Player player, final int top, final int right, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideArmorAndFood) {
            ci.cancel();
        }
    }

    @Inject(method = "extractVehicleHealth", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderMountHealthIfEnabled(@NotNull final GuiGraphicsExtractor context, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideMountHealth) {
            ci.cancel();
        }
    }
}
