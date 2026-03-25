package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.AbstractCollection;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds")
@Pseudo
final class WorkaroundsMixin {
    private WorkaroundsMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(
            method = "findNecessaryWorkarounds",
            at = @At(value = "INVOKE", target = "Ljava/util/EnumSet;add(Ljava/lang/Object;)Z", remap = false),
            remap = false
    )
    private static final boolean darkutils$skipAddIfEnabled(
            @NotNull @Coerce final AbstractCollection<Object> enumSet,
            @NotNull final Object value
    ) {
        if (value instanceof final Enum<?> enumValue) {
            final String name = enumValue.name();

            if (DarkUtilsConfig.INSTANCE.alwaysUseNoErrorContext
                    && "NO_ERROR_CONTEXT_UNSUPPORTED".equals(name)) {
                return false;
            }

            if (DarkUtilsConfig.INSTANCE.reenableAmdGameOptimizations
                    && "AMD_GAME_OPTIMIZATION_BROKEN".equals(name)) {
                return false;
            }
        }

        return enumSet.add(value);
    }
}
