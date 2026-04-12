package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.util.MemoryReserve;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MemoryReserve.class)
final class MemoryReserveMixin {
    private MemoryReserveMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "allocate", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$preventReserveMemoryIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.noMemoryReserve) {
            ci.cancel();
        }
    }
}
