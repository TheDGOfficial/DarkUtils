package gg.darkutils.utils.chat;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public enum SimpleColor {
    BLACK(ChatFormatting.BLACK),
    DARK_BLUE(ChatFormatting.DARK_BLUE),
    DARK_GREEN(ChatFormatting.DARK_GREEN),
    DARK_AQUA(ChatFormatting.DARK_AQUA),
    DARK_RED(ChatFormatting.DARK_RED),
    DARK_PURPLE(ChatFormatting.DARK_PURPLE),
    GOLD(ChatFormatting.GOLD),
    GRAY(ChatFormatting.GRAY),
    DARK_GRAY(ChatFormatting.DARK_GRAY),
    BLUE(ChatFormatting.BLUE),
    GREEN(ChatFormatting.GREEN),
    AQUA(ChatFormatting.AQUA),
    RED(ChatFormatting.RED),
    LIGHT_PURPLE(ChatFormatting.LIGHT_PURPLE),
    YELLOW(ChatFormatting.YELLOW),
    WHITE(ChatFormatting.WHITE);

    @NotNull
    private final ChatFormatting formatting;
    private final int rgb;

    private SimpleColor(@NotNull final ChatFormatting formatting) {
        this.formatting = Objects.requireNonNull(formatting, "SimpleColor formatting value");
        this.rgb = Objects.requireNonNull(this.formatting.getColor(), "SimpleColor formatting color value");
    }

    @NotNull
    public final ChatFormatting toFormatting() {
        return this.formatting;
    }

    public final int toRgb() {
        return this.rgb;
    }
}
