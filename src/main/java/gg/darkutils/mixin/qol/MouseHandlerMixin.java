package gg.darkutils.mixin.qol;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.MouseHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
final class MouseHandlerMixin {
    @Shadow
    private double xpos;
    @Shadow
    private double ypos;

    @Unique
    private double darkutils$prevX;
    @Unique
    private double darkutils$prevY;

    private MouseHandlerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "grabMouse", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I"))
    private final void darkutils$saveCursorPosition(@NotNull final CallbackInfo ci) {
        this.darkutils$prevX = this.xpos;
        this.darkutils$prevY = this.ypos;
    }

    @WrapOperation(method = {"grabMouse", "releaseMouse"}, at = {
            @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabMouse(Lcom/mojang/blaze3d/platform/Window;DD)V"),
            @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;releaseMouse(Lcom/mojang/blaze3d/platform/Window;DD)V")
    }, require = 2)
    private final void darkutils$restoreCursorPositionIfEnabled(@NotNull final Window window, final double x, final double y, @NotNull final Operation<Void> original) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            // SDL uses the requested coordinates to restore the pointer when relative mode ends.
            this.xpos = this.darkutils$prevX;
            this.ypos = this.darkutils$prevY;
            original.call(window, this.xpos, this.ypos);
        } else {
            original.call(window, x, y);
        }
    }
}

