package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.qol.AutoClicker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
final class MinecraftClientMixin {
    private MinecraftClientMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false), method = "render")
    private final void darkutils$skipYieldIfEnabled() {
        if (!DarkUtilsConfig.INSTANCE.disableYield) {
            Thread.yield();
        }
        // skip a yield call that reduces fps, the call was put to make sure rendering does not stall other threads such as chunk loading, but that's OS scheduler's job to handle, the code should utilize maximum resources so this yield call is unnecessary.
    }

    @Inject(at = @At("HEAD"), method = "run")
    private final void darkutils$adjustPriorityIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.alwaysPrioritizeRenderThread) {
            // vanilla game only sets priority to max for processors with 4 or more cores, but it is best to have max priority no matter the core count.
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        }
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private final void darkutils$resetState(@NotNull final CallbackInfo ci) {
        AutoClicker.resetState();
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z"))
    private final boolean darkutils$wasPressed$modifyReturnValueIfApplicable(@NotNull final KeyBinding keyBinding) {
        return AutoClicker.wasPressed(keyBinding);
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private final boolean darkutils$isPressed$modifyReturnValueIfApplicable(@NotNull final KeyBinding keyBinding) {
        return AutoClicker.isPressed(keyBinding);
    }
}
