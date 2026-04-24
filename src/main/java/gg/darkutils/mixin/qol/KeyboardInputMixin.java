package gg.darkutils.mixin.qol;

import gg.darkutils.feat.farming.StickyFarmingKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.KeyboardInput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardInput.class)
final class KeyboardInputMixin {
    private KeyboardInputMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private final boolean darkutils$stickyMovementKeysIfEnabled(@NotNull final KeyMapping keyBinding) {
        return StickyFarmingKeys.isPressed(keyBinding, true);
    }
}
