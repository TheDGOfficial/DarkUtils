package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FrogEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public final class LivingEntityMixin {
    @Unique
    @Final
    private boolean darkutils$playerOrFrog;

    private LivingEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void darkutils$init(@NotNull final CallbackInfo ci) {
        final var entity = (LivingEntity) (Object) this;
        this.darkutils$playerOrFrog = entity.isPlayer() || entity instanceof FrogEntity;
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public final void darkutils$isGlowing$disableIfEnabled(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (DarkUtilsConfig.INSTANCE.disableGlowing && this.darkutils$playerOrFrog) {
            cir.setReturnValue(false);
        }
    }
}
