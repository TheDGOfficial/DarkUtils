package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
final class SynchedEntityDataMixin {
    private SynchedEntityDataMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    // NEW places the callback before allocation, on the serializer mismatch path only.
    @Inject(method = "assignValue", at = @At(value = "NEW", target = "Ljava/lang/IllegalStateException;"), cancellable = true)
    private final void darkutils$skipInvalidMetadata(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.suppressLogspam && LocationUtils.isInHypixel()) {
            ci.cancel();
        }
    }
}
