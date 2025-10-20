package gg.darkutils.utils.chat;

import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public enum BasicFormatting {
    BOLD(Formatting.BOLD),
    ITALIC(Formatting.ITALIC),
    UNDERLINE(Formatting.UNDERLINE),
    STRIKETHROUGH(Formatting.STRIKETHROUGH),
    OBFUSCATED(Formatting.OBFUSCATED);

    @NotNull
    private final Formatting formatting;

    private BasicFormatting(@NotNull final Formatting formatting) {
        this.formatting = formatting;
    }

    @NotNull
    public final Formatting toFormatting() {
        return this.formatting;
    }
}
