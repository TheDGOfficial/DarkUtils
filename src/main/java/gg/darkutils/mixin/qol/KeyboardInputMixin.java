package gg.darkutils.mixin.qol;

import gg.darkutils.feat.farming.StickyFarmingKeys;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
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

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private final boolean darkutils$stickyMovementKeysIfEnabled(@NotNull final KeyBinding keyBinding) {
        return StickyFarmingKeys.isPressed(keyBinding, true);
    }
}
