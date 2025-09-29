package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.util.crash.CrashMemoryReserve;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashMemoryReserve.class)
final class CrashMemoryReserveMixin {
    private CrashMemoryReserveMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "reserveMemory", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$preventReserveMemoryIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.noMemoryReserve) {
            ci.cancel();
        }
    }
}
