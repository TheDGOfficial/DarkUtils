package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
final class MouseMixin {
    @Shadow
    private double x;
    @Shadow
    private double y;

    @Unique
    private double darkutils$prevX;
    @Unique
    private double darkutils$prevY;

    private MouseMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "unlockCursor", at = @At("HEAD"))
    private final void darkutils$saveUnlockCursorPositionIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            this.darkutils$prevX = this.x;
            this.darkutils$prevY = this.y;
        } else {
            // Reset values when turning the feature off
            this.darkutils$prevX = 0.0D;
            this.darkutils$prevY = 0.0D;
        }
    }

    @Inject(method = "lockCursor", at = @At("HEAD"))
    private final void darkutils$saveLockCursorPositionIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            this.darkutils$prevX = this.x;
            this.darkutils$prevY = this.y;
        } else {
            // Reset values when turning the feature off
            this.darkutils$prevX = 0.0D;
            this.darkutils$prevY = 0.0D;
        }
    }

    @Redirect(
            method = "unlockCursor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V"
            )
    )
    private final void darkutils$preventUnlockCursorWarpIfEnabled(final long handler, final int inputModeValue, final double x, final double y) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            // Restore previous x/y to prevent warp
            this.x = this.darkutils$prevX;
            this.y = this.darkutils$prevY;

            // Only set input mode so cursor is visible
            GLFW.glfwSetInputMode(handler, GLFW.GLFW_CURSOR, inputModeValue);
        } else {
            InputUtil.setCursorParameters(handler, inputModeValue, x, y);
        }
    }

    @Redirect(
            method = "lockCursor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V"
            )
    )
    private final void darkutils$preventLockCursorWarpIfEnabled(final long handler, final int inputModeValue, final double x, final double y) {
        if (DarkUtilsConfig.INSTANCE.neverResetCursorPosition) {
            // Restore previous x/y to prevent warp
            this.x = this.darkutils$prevX;
            this.y = this.darkutils$prevY;

            // Only set input mode so cursor is grabbed
            GLFW.glfwSetInputMode(handler, GLFW.GLFW_CURSOR, inputModeValue);
        } else {
            InputUtil.setCursorParameters(handler, inputModeValue, x, y);
        }
    }
}

