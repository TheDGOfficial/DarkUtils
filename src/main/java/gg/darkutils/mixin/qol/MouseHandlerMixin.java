package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.MouseHandler;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "releaseMouse", at = @At("HEAD"))
    private final void darkutils$saveUnlockCursorPositionIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            this.darkutils$prevX = this.xpos;
            this.darkutils$prevY = this.ypos;
        } else {
            // Reset values when turning the feature off
            this.darkutils$prevX = 0.0D;
            this.darkutils$prevY = 0.0D;
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"))
    private final void darkutils$saveLockCursorPositionIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            this.darkutils$prevX = this.xpos;
            this.darkutils$prevY = this.ypos;
        } else {
            // Reset values when turning the feature off
            this.darkutils$prevX = 0.0D;
            this.darkutils$prevY = 0.0D;
        }
    }

    @Redirect(
            method = "releaseMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V"
            )
    )
    private final void darkutils$preventUnlockCursorWarpIfEnabled(@NotNull final Window window, final int inputModeValue, final double x, final double y) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            // Restore previous x/y to prevent warp
            this.xpos = this.darkutils$prevX;
            this.ypos = this.darkutils$prevY;

            // Only set input mode so cursor is visible
            GLFW.glfwSetInputMode(window.handle(), GLFW.GLFW_CURSOR, inputModeValue);
        } else {
            InputConstants.grabOrReleaseMouse(window, inputModeValue, x, y);
        }
    }

    @Redirect(
            method = "grabMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V"
            )
    )
    private final void darkutils$preventLockCursorWarpIfEnabled(@NotNull final Window window, final int inputModeValue, final double x, final double y) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            // Restore previous x/y to prevent warp
            this.xpos = this.darkutils$prevX;
            this.ypos = this.darkutils$prevY;

            // Only set input mode so cursor is grabbed
            GLFW.glfwSetInputMode(window.handle(), GLFW.GLFW_CURSOR, inputModeValue);
        } else {
            InputConstants.grabOrReleaseMouse(window, inputModeValue, x, y);
        }
    }
}

