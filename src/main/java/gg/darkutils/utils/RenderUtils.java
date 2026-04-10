package gg.darkutils.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.gizmos.Gizmos;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
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
    private static final @NotNull Supplier<Map<ChatFormatting, TextColor>> FORMATTING_TO_TEXT_COLOR = LazyConstants.lazyConstantOf(() -> LazyConstants.lazyMapOf(
            Set.copyOf(Arrays.asList(ChatFormatting.values())),
            formatting -> Objects.requireNonNull(
                    TextColor.fromLegacyFormat(formatting),
                    "Formatting must convert to TextColor"
            )
    ));
    /**
     * Holds the item to empty ItemStack cache.
     */
    private static final @NotNull Supplier<Map<Item, ItemStack>> ITEM_TO_ITEM_STACK = LazyConstants.lazyConstantOf(() -> LazyConstants.lazyMapOf(
            Set.copyOf(BuiltInRegistries.ITEM.stream().toList()),
            ItemStack::new
    ));
    /**
     * Holds the empty OrderedText.
     */
    private static final @NotNull Supplier<FormattedCharSequence> EMPTY_ORDERED_TEXT = LazyConstants.lazyConstantOf(() -> Component.nullToEmpty("").getVisualOrderText());

    private RenderUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    @NotNull
    public static final RenderUtils.RenderingText createRenderingText() {
        return new RenderUtils.RenderingText();
    }

    @NotNull
    public static final RenderUtils.RenderingText createRenderingText(@NotNull final String initial) {
        return new RenderUtils.RenderingText(initial);
    }

    private static final int getMiddleOfScreenYCoordinate() {
        final var client = Minecraft.getInstance();
        return (client.getWindow().getGuiScaledHeight() >> 1) - (client.font.lineHeight >> 1);
    }

    private static final int convertFormattingToRGBA(@NotNull final ChatFormatting color) {
        return RenderUtils.FORMATTING_TO_TEXT_COLOR.get().get(color).getValue();
    }

    private static final int convertFormattingToOpaqueColor(@NotNull final ChatFormatting color) {
        // set alpha to 255 otherwise will be invisible
        return 0xFF00_0000 | RenderUtils.convertFormattingToRGBA(color);
    }

    private static final int toARGB(final double alpha, final double red, final double green, final double blue) {
        final var roundedAlpha = (int) Math.round(alpha * 255.0D) & 0xFF;
        final var roundedRed = (int) Math.round(red * 255.0D) & 0xFF;
        final var roundedGreen = (int) Math.round(green * 255.0D) & 0xFF;
        final var roundedBlue = (int) Math.round(blue * 255.0D) & 0xFF;

        return roundedAlpha << 24 | roundedRed << 16 | roundedGreen << 8 | roundedBlue;
    }

    public static final void renderText(@NotNull final GuiGraphics context, @NotNull final RenderUtils.RenderingText text, final int x, final int y, @NotNull final ChatFormatting color) {
        RenderUtils.renderText(context, text, x, () -> y, color);
    }

    public static final void renderText(@NotNull final GuiGraphics context, @NotNull final RenderUtils.RenderingText text, final int x, @NotNull final IntSupplier y, @NotNull final ChatFormatting color) {
        RenderUtils.renderText(context, text, () -> x, y, color);
    }

    private static final void renderText(@NotNull final GuiGraphics context, @NotNull final RenderUtils.RenderingText text, final IntSupplier x, @NotNull final IntSupplier y, @NotNull final ChatFormatting color) {
        context.drawString(Minecraft.getInstance().font, text.orderedText, x.getAsInt(), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final int middleAlignedXForText(@NotNull final RenderUtils.RenderingText text) {
        return (Minecraft.getInstance().getWindow().getGuiScaledWidth() >> 1) - (text.getWidth() >> 1);
    }

    public static final void renderCenteredText(@NotNull final GuiGraphics context, @NotNull final RenderUtils.RenderingText text, @NotNull final IntSupplier y, @NotNull final ChatFormatting color) {
        context.drawString(Minecraft.getInstance().font, text.orderedText, RenderUtils.middleAlignedXForText(text), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final void renderItem(@NotNull final GuiGraphics context, @NotNull final Item item, final int x, final int y) {
        RenderUtils.renderItem(context, item, x, () -> y);
    }

    private static final void renderItem(@NotNull final GuiGraphics context, @NotNull final Item item, final int x, @NotNull final IntSupplier y) {
        RenderUtils.renderItem(context, item, () -> x, y);
    }

    private static final void renderItem(@NotNull final GuiGraphics context, @NotNull final Item item, final IntSupplier x, @NotNull final IntSupplier y) {
        context.renderFakeItem(RenderUtils.ITEM_TO_ITEM_STACK.get().get(item), x.getAsInt(), y.getAsInt(), 0);
    }

    public static final void drawBlockOutline(@NotNull final WorldRenderContext context,
                                              @NotNull final BlockPos pos,
                                              @NotNull final ChatFormatting color) {
        // Convert Formatting to RGBA floats
        final var rgb = RenderUtils.convertFormattingToRGBA(color);

        final var red = (rgb >> 16 & 0xFF) / 255.0D;
        final var green = (rgb >> 8 & 0xFF) / 255.0D;
        final var blue = (rgb & 0xFF) / 255.0D;
        final var alpha = 1.0D; // fully opaque

        final var argb = RenderUtils.toARGB(alpha, red, green, blue);

        final var style = GizmoStyle.stroke(argb);

        final var immutablePos = pos.immutable();

        Gizmos.cuboid(AABB.encapsulatingFullBlocks(immutablePos, immutablePos), style);
    }

    /**
     * Checks if the current thread is not the render thread and returns true if so.
     * <p>
     * Typically used to re-schedule an action next tick to ensure thread-safety when called from external thread,
     * if you need to fail-fast and reject external thread call, use {@link RenderUtils#validateRenderThread()} instead.
     */
    public static final boolean isNotCallingFromRenderThread() {
        return !RenderSystem.isOnRenderThread();
    }

    /**
     * Checks if the current thread is not the render thread and throws {@link IllegalStateException} if so.
     */
    public static final void validateRenderThread() {
        if (RenderUtils.isNotCallingFromRenderThread()) {
            throw new IllegalStateException("unexpected caller thread with name: " + Thread.currentThread().getName() + ", expected: Render thread");
        }
    }

    public static final class RenderingText {
        @NotNull
        private String text;

        @NotNull
        private FormattedCharSequence orderedText;

        private int width;
        private boolean widthDirty = true;

        private RenderingText() {
            this("", RenderUtils.EMPTY_ORDERED_TEXT.get());
        }

        private RenderingText(@NotNull final String text) {
            this(text, Component.nullToEmpty(text).getVisualOrderText());
        }

        private RenderingText(@NotNull final String text, @NotNull final FormattedCharSequence orderedText) {
            super();

            this.text = text;
            this.orderedText = orderedText;
        }

        public final void setText(@NotNull final String newText) {
            if (this.text.equals(newText)) {
                return;
            }

            this.text = newText;
            this.orderedText = Component.nullToEmpty(newText).getVisualOrderText();

            this.widthDirty = true;
        }

        private final int getWidth() {
            if (this.widthDirty) {
                this.widthDirty = false;
                return this.width = Minecraft.getInstance().font.width(this.orderedText);
            }

            return this.width;
        }

        @Override
        public final String toString() {
            return "RenderingText{" +
                    "text='" + this.text + '\'' +
                    ", orderedText=" + this.orderedText +
                    ", width=" + this.width +
                    ", widthDirty=" + this.widthDirty +
                    '}';
        }
    }
}
