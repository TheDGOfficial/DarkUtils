package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerSocialManager.class)
final class PlayerSocialManagerMixin {
    private PlayerSocialManagerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = {"addPlayer", "removePlayer"}, at = @At("HEAD"), cancellable = true, require = 2)
    private final void darkutils$skipSocialPlayerUpdate(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.disableSocialInteractions) {
            ci.cancel();
        }
    }
}
