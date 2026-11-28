package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.resources.SkinManager$1")
final class SkinManagerMixin {
    private SkinManagerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @WrapWithCondition(method = "lambda$load$0", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private static final boolean darkutils$logInvalidTextureSignature(@NotNull final Logger logger, @NotNull final String message, @NotNull final Object profileId) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }
}
