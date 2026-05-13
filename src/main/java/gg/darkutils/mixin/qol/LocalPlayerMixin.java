package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
final class LocalPlayerMixin {
    private LocalPlayerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"
            )
    )
    private final boolean darkutils$alwaysSprintIfEnabled(@NotNull final Input input) {
        if (DarkUtilsConfig.INSTANCE.alwaysSprint) {
            final var player = Minecraft.getInstance().player;

            final var water = null != player && player.isInWater();
            final var beans = Helpers.doesTargetedBlockMatch(Helpers.isCocoaBeans());

            return !water && (!beans || !LocationUtils.isInGarden());
        }

        return input.sprint();
    }
}
