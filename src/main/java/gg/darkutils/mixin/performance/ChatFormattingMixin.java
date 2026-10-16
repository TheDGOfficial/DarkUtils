package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.mixinquirks.HolderFields;
import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.regex.Pattern;

@Mixin(ChatFormatting.class)
final class ChatFormattingMixin {
    private ChatFormattingMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Shadow
    @Final
    private static Pattern STRIP_FORMATTING_PATTERN;

    @Overwrite
    @Nullable
    @Contract("!null->!null;_->_")
    public static final String stripFormatting(@Nullable final String input) {
        if (input == null) {
            return null;
        }

        return DarkUtilsConfig.INSTANCE.optimizeStripFormatting ? ChatUtils.removeControlCodes(input) : ChatFormattingMixin.STRIP_FORMATTING_PATTERN.matcher(input).replaceAll("");
    }

    @Redirect(method = "getByCode", at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;values()[Lnet/minecraft/ChatFormatting;"))
    @NotNull
    private static final ChatFormatting @NotNull [] darkutils$values() {
        return DarkUtilsConfig.INSTANCE.optimizeEnumValues ? HolderFields.FormattingCache.FORMATTING_VALUES : ChatFormatting.values();
    }
}
