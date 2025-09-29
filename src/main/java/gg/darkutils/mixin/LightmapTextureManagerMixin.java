package gg.darkutils.mixin;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
final class LightmapTextureManagerMixin {
    private LightmapTextureManagerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private final void darkutils$stopLightUpdatesIfEnabled(final float tickProgress, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.stopLightUpdates) {
            ci.cancel();
        }
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private final float darkutils$fullbrightIfEnabled(final float first, final float second) {
        return Math.max(first, DarkUtilsConfig.INSTANCE.fullbright ? Float.MAX_VALUE : second);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z", ordinal = 0))
    private final boolean darkutils$overrideNightVisionIfEnabled(@NotNull final ClientPlayerEntity player, @NotNull final RegistryEntry<StatusEffect> effect) {
        if (StatusEffects.NIGHT_VISION == effect) {
            return DarkUtilsConfig.INSTANCE.nightVision || player.hasStatusEffect(effect);
        }

        // Won't be the case unless vanilla moved hasStatusEffect around messing with our ordinal but better be safe
        DarkUtils.LOGGER.warn(DarkUtils.MOD_ID + ": {} needs updating ordinal for hasStatusEffect. Please report this!", LightmapTextureManagerMixin.class.getSimpleName());
        return player.hasStatusEffect(effect);
    }
}
