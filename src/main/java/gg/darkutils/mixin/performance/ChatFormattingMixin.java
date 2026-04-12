package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.mixinquirks.HolderFields;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatFormatting.class)
final class ChatFormattingMixin {
    private ChatFormattingMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "getByCode", at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;values()[Lnet/minecraft/ChatFormatting;"))
    @NotNull
    private static final ChatFormatting @NotNull [] darkutils$values() {
        return DarkUtilsConfig.INSTANCE.optimizeEnumValues ? HolderFields.FormattingCache.FORMATTING_VALUES : ChatFormatting.values();
    }
}
