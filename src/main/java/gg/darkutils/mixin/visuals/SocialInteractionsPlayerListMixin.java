package gg.darkutils.mixin.visuals;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.client.gui.screens.social.SocialInteractionsPlayerList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.regex.Pattern;

@Mixin(SocialInteractionsPlayerList.class)
final class SocialInteractionsPlayerListMixin {
    @Unique
    private static final Pattern DARKUTILS$USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");

    private SocialInteractionsPlayerListMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "updatePlayerList", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipPlayerListConstruction(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.disableSocialInteractions) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "addOnlinePlayers", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private final boolean darkutils$showPlayer(@NotNull final Map<?, ?> entries, @NotNull final Object uuid, @NotNull final Object entry) {
        return !DarkUtilsConfig.INSTANCE.hideFakePlayersInSocialInteractions || !LocationUtils.isInSkyblock()
                || DARKUTILS$USERNAME_PATTERN.matcher(((PlayerEntry) entry).getPlayerName()).matches();
    }
}
