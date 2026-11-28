package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.world.entity.projectile.FishingHook;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHook.class)
final class FishingHookMixin {
    private FishingHookMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @WrapWithCondition(method = "recreateFromPacket", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private static final boolean darkutils$logInvalidOwner(@NotNull final Logger logger, @NotNull final String message, @NotNull final Object owner, @NotNull final Object entityId) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }
}
