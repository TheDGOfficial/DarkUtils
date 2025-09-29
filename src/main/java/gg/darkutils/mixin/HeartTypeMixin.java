package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net/minecraft/client/gui/hud/InGameHud$HeartType")
final class HeartTypeMixin {
    private HeartTypeMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    /**
     * Redirects the hasStatusEffect call inside HeartType.fromPlayerState.
     * Returns false when the effect being checked is WITHER and the config option is enabled, otherwise
     * passes the original call through unchanged.
     */
    @Redirect(
            method = "fromPlayerState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"
            )
    )
    private static final boolean darkutils$disableWitherOverlayIfEnabled(@NotNull final PlayerEntity player, @NotNull final RegistryEntry<StatusEffect> effectEntry) {
        return (effectEntry != StatusEffects.WITHER || !DarkUtilsConfig.INSTANCE.noWitherHearts) && player.hasStatusEffect(effectEntry); // Prevents wither hearts
    }
}
