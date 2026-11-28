package gg.darkutils.mixin.performance;

import com.mojang.text2speech.Narrator;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.NarratorStatus;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameNarrator.class)
final class GameNarratorMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/text2speech/Narrator;getNarrator()Lcom/mojang/text2speech/Narrator;", remap = false))
    private final @NotNull Narrator darkutils$useSilentNarrator() {
        return Narrator.EMPTY;
    }

    @Inject(method = "getStatus", at = @At("HEAD"), cancellable = true)
    private final void darkutils$forceNarratorOff(@NotNull final CallbackInfoReturnable<NarratorStatus> cir) {
        cir.setReturnValue(NarratorStatus.OFF);
    }

    // Skip the native initialization warning and toggle announcements for an intentionally disabled backend.
    @Inject(method = {"checkStatus", "updateNarratorStatus"}, at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipNarratorStatusMessages(@NotNull final CallbackInfo ci) {
        ci.cancel();
    }
}
