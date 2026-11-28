package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
final class YggdrasilMinecraftSessionServiceMixin {
    private YggdrasilMinecraftSessionServiceMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @WrapWithCondition(method = "unpackTextures", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", ordinal = 0))
    private final boolean darkutils$logTextureDecodeFailure(@NotNull final Logger logger, @NotNull final String message, @NotNull final Throwable failure) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }
}
