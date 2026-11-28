package gg.darkutils.mixin.performance;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
final class ScreenNarrationMixin {
    // The UI narration debug flag can bypass the backend availability check.
    @Inject(method = "shouldRunNarration", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipScreenNarration(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
