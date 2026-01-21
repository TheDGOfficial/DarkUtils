package gg.darkutils.utils;

import gg.darkutils.DarkUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class RenderUtils {
    /**
     * Chat aligned x position, vanilla uses this x value when rendering text in chat.
     */
    public static final int CHAT_ALIGNED_X = 2;
    /**
     * Middle aligned y position, calculating based on current window size.
     */
    public static final @NotNull IntSupplier MIDDLE_ALIGNED_Y = RenderUtils::getMiddleOfScreenYCoordinate;
    /**
     * Holds the formatting to text color cache.
     */
    private static final @NotNull Supplier<Map<Formatting, TextColor>> FORMATTING_TO_TEXT_COLOR = LazyConstants.lazyConstantOf(() -> LazyConstants.lazyMapOf(
            Set.copyOf(Arrays.asList(Formatting.values())),
            formatting -> Objects.requireNonNull(
                    TextColor.fromFormatting(formatting),
                    "Formatting must convert to TextColor"
            )
    ));
    /**
     * Holds the item to empty ItemStack cache.
     */
    private static final @NotNull Map<Item, ItemStack> ITEM_TO_ITEM_STACK = HashMap.newHashMap(16);
    /**
     * Holds the string to ordered text cache.
     */
    private static final @NotNull Map<String, OrderedText> ORDERED_TEXT_CACHE = HashMap.newHashMap(32);

    static {
        // Clear the cache 2 tick after every other second
        TickUtils.queueRepeatingTickTask(RenderUtils.ORDERED_TEXT_CACHE::clear, 42);
    }

    private RenderUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final int getMiddleOfScreenYCoordinate() {
        final var client = MinecraftClient.getInstance();
        return (client.getWindow().getScaledHeight() >> 1) - (client.textRenderer.fontHeight >> 1);
    }

    private static final int convertFormattingToRGBA(@NotNull final Formatting color) {
        return RenderUtils.FORMATTING_TO_TEXT_COLOR.get().get(color).getRgb();
    }

    private static final int convertFormattingToOpaqueColor(@NotNull final Formatting color) {
        // set alpha to 255 otherwise will be invisible
        return 0xFF00_0000 | RenderUtils.convertFormattingToRGBA(color);
    }

    private static final int toARGB(final double alpha, final double red, final double green, final double blue) {
        final int a = (int) Math.round(alpha * 255.0D) & 0xFF;
        final int r = (int) Math.round(red * 255.0D) & 0xFF;
        final int g = (int) Math.round(green * 255.0D) & 0xFF;
        final int b = (int) Math.round(blue * 255.0D) & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final int x, final int y, @NotNull final Formatting color) {
        RenderUtils.renderText(context, text, x, () -> y, color);
    }

    public static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final int x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        RenderUtils.renderText(context, text, () -> x, y, color);
    }

    private static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final IntSupplier x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        context.drawText(MinecraftClient.getInstance().textRenderer, RenderUtils.ORDERED_TEXT_CACHE.computeIfAbsent(text, t -> Text.of(t).asOrderedText()), x.getAsInt(), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final int middleAlignedXForText(@NotNull final String text) {
        final var client = MinecraftClient.getInstance();
        final var textRenderer = client.textRenderer;

        final var textWidth = textRenderer.getWidth(text);

        return (client.getWindow().getScaledWidth() >> 1) - (textWidth >> 1);
    }

    public static final void renderCenteredText(@NotNull final DrawContext context, @NotNull final String text, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        context.drawText(MinecraftClient.getInstance().textRenderer, RenderUtils.ORDERED_TEXT_CACHE.computeIfAbsent(text, t -> Text.of(t).asOrderedText()), RenderUtils.middleAlignedXForText(text), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final void renderItem(@NotNull final DrawContext context, @NotNull final Item item, final int x, final int y) {
        RenderUtils.renderItem(context, item, x, () -> y);
    }

    private static final void renderItem(@NotNull final DrawContext context, @NotNull final Item item, final int x, @NotNull final IntSupplier y) {
        RenderUtils.renderItem(context, item, () -> x, y);
    }

    private static final void renderItem(@NotNull final DrawContext context, @NotNull final Item item, final IntSupplier x, @NotNull final IntSupplier y) {
        context.drawItemWithoutEntity(RenderUtils.ITEM_TO_ITEM_STACK.computeIfAbsent(item, ItemStack::new), x.getAsInt(), y.getAsInt(), 0);
    }

    public static final void drawBlockOutline(@NotNull final WorldRenderContext context,
                                              @NotNull final BlockPos pos,
                                              @NotNull final Formatting color) {
        // Convert Formatting to RGBA floats
        final var rgb = RenderUtils.convertFormattingToRGBA(color);

        final var red = (rgb >> 16 & 0xFF) / 255.0D;
        final var green = (rgb >> 8 & 0xFF) / 255.0D;
        final var blue = (rgb & 0xFF) / 255.0D;
        final var alpha = 1.0D; // fully opaque

        final int argb = RenderUtils.toARGB(alpha, red, green, blue);

        DrawStyle style = DrawStyle.stroked(argb);

        final var immutablePos = pos.toImmutable();

        GizmoDrawing.box(Box.enclosing(immutablePos, immutablePos), style);
    }
}
