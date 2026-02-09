package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
final class EntityMixin {
    private EntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public final void darkutils$isGlowing$disableIfEnabled(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (DarkUtilsConfig.INSTANCE.disableGlowing) {
            cir.setReturnValue(false);
        }
    }
}
