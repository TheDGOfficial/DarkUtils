package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Scoreboard.class)
final class ScoreboardMixin {
    private ScoreboardMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @WrapWithCondition(method = "addPlayerTeam", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private final boolean darkutils$logDuplicateTeam(@NotNull final Logger logger, @NotNull final String message, @NotNull final Object name) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }
}
