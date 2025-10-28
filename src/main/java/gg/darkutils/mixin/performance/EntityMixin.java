package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public final class EntityMixin {
    private EntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public final void darkutils$isGlowing$disableIfEnabled(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        final var entity = (Entity) (Object) this;
        if (DarkUtilsConfig.INSTANCE.disableGlowing && entity instanceof ItemEntity) {
            cir.setReturnValue(false);
        }
    }
}
