package gg.darkutils.utils.chat;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

public enum SimpleFormatting {
    BOLD(ChatFormatting.BOLD),
    ITALIC(ChatFormatting.ITALIC),
    UNDERLINE(ChatFormatting.UNDERLINE),
    STRIKETHROUGH(ChatFormatting.STRIKETHROUGH),
    OBFUSCATED(ChatFormatting.OBFUSCATED);

    @NotNull
    private final ChatFormatting formatting;

    private SimpleFormatting(@NotNull final ChatFormatting formatting) {
        this.formatting = formatting;
    }

    @NotNull
    public final ChatFormatting toFormatting() {
        return this.formatting;
    }
}
