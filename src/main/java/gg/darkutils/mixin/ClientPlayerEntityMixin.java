package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
final class ClientPlayerEntityMixin {
    private ClientPlayerEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/PlayerInput;sprint()Z"
            )
    )
    private final boolean darkutils$alwaysSprintIfEnabled(@NotNull final PlayerInput input) {
        return DarkUtilsConfig.INSTANCE.alwaysSprint || input.sprint();
    }
}
