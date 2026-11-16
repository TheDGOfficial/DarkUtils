package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
final class CampfireBlockEntityMixin {
    private CampfireBlockEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(
            method = "particleTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private static final void darkutils$disableCampfireSmokeParticlesIfEnabled(
            @NotNull final Level level,
            @NotNull final BlockPos pos,
            @NotNull final BlockState state,
            @NotNull final CampfireBlockEntity campfire,
            @NotNull final CallbackInfo ci
    ) {
        if (DarkUtilsConfig.INSTANCE.disableCampfireSmokeParticles) {
            // cancel campfire particle ticking
            ci.cancel();
        }
    }
}
