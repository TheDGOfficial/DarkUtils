package gg.darkutils.mixin.qol;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
final class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Nullable
    private Screen screen;

    private GuiMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "addSocialInteractionsToast", at = @At("HEAD"), cancellable = true)
    private final void darkutils$hideSocialInteractionsToast(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.disableSocialInteractions) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "handleKeybinds",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keySocialInteractions:Lnet/minecraft/client/KeyMapping;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 0))
    private final boolean darkutils$allowSocialInteractionsShortcut(final boolean pressed) {
        return pressed && !DarkUtilsConfig.INSTANCE.disableSocialInteractions;
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private final void darkutils$blockSocialInteractionsScreen(@Nullable final Screen nextScreen, @NotNull final CallbackInfo ci) {
        if (nextScreen instanceof SocialInteractionsScreen && DarkUtilsConfig.INSTANCE.disableSocialInteractions) {
            ci.cancel();
        }
    }

    // Keep the existing grab only when opening a transfer screen. Every other screen
    // (including the screen shown on a timeout) still gets vanilla's release.
    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;releaseMouse()V"))
    private final boolean darkutils$releaseMouseForScreen(@NotNull final MouseHandler mouse) {
        return !(DarkUtilsConfig.INSTANCE.seamlessHypixelTransfers && LocationUtils.isInHypixel()
                && this.minecraft.isWindowActive() && mouse.isMouseGrabbed()
                && (this.screen instanceof LevelLoadingScreen || this.screen instanceof ServerReconfigScreen));
    }
}
