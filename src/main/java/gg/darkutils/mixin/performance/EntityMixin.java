package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public final class EntityMixin {
    @Unique
    @Final
    private boolean darkutils$item;

    private EntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void darkutils$init(@NotNull final CallbackInfo ci) {
        final var entity = (Entity) (Object) this;
        this.darkutils$item = entity instanceof ItemEntity;
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public final void darkutils$isGlowing$disableIfEnabled(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (DarkUtilsConfig.INSTANCE.disableGlowing && this.darkutils$item) {
            cir.setReturnValue(false);
        }
    }
}
