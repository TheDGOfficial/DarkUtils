package gg.darkutils.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.IntSupplier;

public final class RenderUtils {
    /**
     * Chat aligned x position, vanilla uses this x value when rendering text in chat.
     */
    public static final int CHAT_ALIGNED_X = 2;
    /**
     * Middle aligned y position, calculating based on current window size.
     */
    public static final @NotNull IntSupplier MIDDLE_ALIGNED_Y = RenderUtils::getMiddleOfScreenYCoords;

    private RenderUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final int getMiddleOfScreenYCoords() {
        final var client = MinecraftClient.getInstance();
        return (client.getWindow().getScaledHeight() >> 1) - (client.textRenderer.fontHeight >> 1);
    }

    private static final int convertFormattingToOpaqueColor(@NotNull final Formatting color) {
        // set alpha to 255 otherwise the text will be invisible
        return 0xFF00_0000 | Objects.requireNonNull(TextColor.fromFormatting(color), "formatting to text color must not be null").getRgb();
    }

    public static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final int x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        RenderUtils.renderText(context, text, () -> x, y, color);
    }

    public static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final IntSupplier x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        context.drawText(MinecraftClient.getInstance().textRenderer, Text.of(text).asOrderedText(), x.getAsInt(), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final void renderCenteredText(@NotNull final DrawContext context, @NotNull final String text, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        final var client = MinecraftClient.getInstance();
        final var textRenderer = client.textRenderer;

        final var textWidth = textRenderer.getWidth(text);
        final var centerX = (client.getWindow().getScaledWidth() >> 1) - (textWidth >> 1);

        context.drawText(textRenderer, Text.of(text).asOrderedText(), centerX, y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }
}
