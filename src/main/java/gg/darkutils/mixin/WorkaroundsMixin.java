package gg.darkutils.mixin;

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
    private static boolean darkutils$skipAddIfEnabled(
            @NotNull @Coerce final AbstractCollection<Object> enumSet,
            @NotNull final Object value
    ) {
        return (!DarkUtilsConfig.INSTANCE.alwaysUseNoErrorContext || !(value instanceof final Enum<?> enumValue) || !"NO_ERROR_CONTEXT_UNSUPPORTED".equals(enumValue.name())) && enumSet.add(value);
    }
}
