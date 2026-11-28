package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Shadow
    @Final
    protected Minecraft minecraft;

    private ScreenMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private final void darkutils$releaseMouseOnInterruptedTransfer(@NotNull final CallbackInfo ci) {
        if (((Object) this instanceof LevelLoadingScreen || (Object) this instanceof ServerReconfigScreen)
                && this.minecraft.mouseHandler.isMouseGrabbed()
                && (!DarkUtilsConfig.INSTANCE.seamlessHypixelTransfers || !LocationUtils.isInHypixel() || !this.minecraft.isWindowActive())) {
            this.minecraft.mouseHandler.releaseMouse();
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private final void darkutils$hideReconfigurationScreen(@NotNull final CallbackInfo ci) {
        if ((Object) this instanceof ServerReconfigScreen && DarkUtilsConfig.INSTANCE.seamlessHypixelTransfers
                && LocationUtils.isInHypixel() && this.minecraft.isWindowActive() && this.minecraft.mouseHandler.isMouseGrabbed()) {
            ci.cancel();
        }
    }
}
