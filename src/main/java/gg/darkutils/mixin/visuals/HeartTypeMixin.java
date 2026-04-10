package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.Gui$HeartType")
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
            method = "forPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"
            )
    )
    private static final boolean darkutils$disableWitherOverlayIfEnabled(@NotNull final Player player, @NotNull final Holder<MobEffect> effectEntry) {
        return (!DarkUtilsConfig.INSTANCE.noWitherHearts || effectEntry != MobEffects.WITHER) && player.hasEffect(effectEntry); // Prevents wither hearts
    }
}
