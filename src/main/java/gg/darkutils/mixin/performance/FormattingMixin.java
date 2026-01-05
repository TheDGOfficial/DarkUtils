package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Formatting.class)
final class FormattingMixin {
    @Unique
    @NotNull
    private static final Formatting @NotNull [] darkutils$values = Formatting.values();

    private FormattingMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "byCode", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Formatting;values()[Lnet/minecraft/util/Formatting;"))
    @NotNull
    private static final Formatting @NotNull [] darkutils$values() {
        return DarkUtilsConfig.INSTANCE.optimizeEnumValues ? FormattingMixin.darkutils$values : Formatting.values();
    }
}
