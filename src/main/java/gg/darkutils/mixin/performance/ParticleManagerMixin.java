package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
final class ParticleManagerMixin {
    private ParticleManagerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void darkutils$disableCampfireSmokeParticlesIfEnabled(
            @NotNull final ParticleEffect effect, final double x, final double y, final double z,
            final double velocityX, final double velocityY, final double velocityZ,
            @NotNull final CallbackInfoReturnable<Particle> cir
    ) {
        if (DarkUtilsConfig.INSTANCE.disableCampfireSmokeParticles) {
            final var type = effect.getType();
            if (ParticleTypes.CAMPFIRE_COSY_SMOKE == type || ParticleTypes.CAMPFIRE_SIGNAL_SMOKE == type) {
                // cancel creation
                cir.setReturnValue(null);
            }
        }
    }
}
