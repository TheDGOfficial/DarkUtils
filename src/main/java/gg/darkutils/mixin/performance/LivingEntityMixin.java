package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FrogEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public final class LivingEntityMixin {
    private LivingEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public final void darkutils$isGlowing$disableIfEnabled(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        final var entity = (LivingEntity) (Object) this;
        if (DarkUtilsConfig.INSTANCE.disableGlowing && (entity.isPlayer() || entity instanceof FrogEntity)) {
            cir.setReturnValue(false);
        }
    }
}
