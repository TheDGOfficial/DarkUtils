package gg.darkutils.utils.chat;

import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public enum BasicColor {
    BLACK(Formatting.BLACK),
    DARK_BLUE(Formatting.DARK_BLUE),
    DARK_GREEN(Formatting.DARK_GREEN),
    DARK_AQUA(Formatting.DARK_AQUA),
    DARK_RED(Formatting.DARK_RED),
    DARK_PURPLE(Formatting.DARK_PURPLE),
    GOLD(Formatting.GOLD),
    GRAY(Formatting.GRAY),
    DARK_GRAY(Formatting.DARK_GRAY),
    BLUE(Formatting.BLUE),
    GREEN(Formatting.GREEN),
    AQUA(Formatting.AQUA),
    RED(Formatting.RED),
    LIGHT_PURPLE(Formatting.LIGHT_PURPLE),
    YELLOW(Formatting.YELLOW),
    WHITE(Formatting.WHITE);

    @NotNull
    private final Formatting formatting;

    private BasicColor(@NotNull final Formatting formatting) {
        this.formatting = formatting;
    }

    @NotNull
    public final Formatting toFormatting() {
        return this.formatting;
    }

    public final int toRgb() {
        return Objects.requireNonNull(this.formatting.getColorValue(), "BasicColor formatting color value");
    }
}
