package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.LightningEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightningEntity.class)
final class LightningEntityMixin {
    private LightningEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    /**
     * Redirects both thunder and impact sound calls in LightningEntity#tick
     * to suppress them completely.
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSoundClient(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"
            )
    )
    private final void darkutils$skipLightningBoltSoundsIfEnabled(
            @NotNull final World world,
            final double x,
            final double y,
            final double z,
            @NotNull final SoundEvent sound,
            @NotNull final SoundCategory category,
            final float volume,
            final float pitch,
            final boolean useDistance
    ) {
        if (!DarkUtilsConfig.INSTANCE.noLightningBolts) {
            world.playSoundClient(x, y, z, sound, category, volume, pitch, useDistance);
        }
        // Suppressed: do nothing instead of playing thunder/impact sounds
    }
}
