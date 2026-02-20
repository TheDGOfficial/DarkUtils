package gg.darkutils.utils;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import gg.darkutils.DarkUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexRendering;
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
     * Represents the render pipeline that does culling to hide when out of screen and suitable for rendering lines.
     */
    private static final @NotNull RenderPipeline OUTLINE_CULL_RENDER_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(DarkUtils.MOD_ID, "pipeline/" + DarkUtils.MOD_ID + "_outline_cull"))
            .build());
    /**
     * Represents the custom line width parameter.
     */
    private static final @NotNull RenderLayer.MultiPhaseParameters.Builder LINES_PARAMETER = RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3.69)));
    /**
     * Represents the box outline layer.
     */
    private static final @NotNull RenderLayer.MultiPhase BOX_OUTLINE_LAYER = RenderLayer.of(
            DarkUtils.MOD_ID + "_box_outline",
            RenderLayer.DEFAULT_BUFFER_SIZE,
            false,
            false,
            RenderUtils.OUTLINE_CULL_RENDER_PIPELINE,
            RenderUtils.LINES_PARAMETER.build(false)
    );
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
     * Holds the empty OrderedText.
     */
    private static final @NotNull OrderedText EMPTY_ORDERED_TEXT = Text.of("").asOrderedText();

    public static final class RenderingText {
        @NotNull
        private String text;

        @NotNull
        private OrderedText orderedText;

        private WidthHolder widthHolder;

        private RenderingText() {
            this("", RenderUtils.EMPTY_ORDERED_TEXT);
        }

        private RenderingText(@NotNull final String text) {
            this(text, Text.of(text).asOrderedText());
        }

        private RenderingText(@NotNull final String text, @NotNull final OrderedText orderedText) {
            super();

            this.text = text;
            this.orderedText = orderedText;

            this.widthHolder = new WidthHolder(this);
        }

        public final void setText(@NotNull final String newText) {
            if (this.text.equals(newText)) {
                return;
            }

            this.text = newText;
            this.orderedText = Text.of(newText).asOrderedText();

            this.widthHolder.markDirty();
        }
    }

    private static final class WidthHolder {
        @NotNull
        private final RenderingText renderingText;

        private WidthHolder(@NotNull final RenderingText renderingText) {
            this.renderingText = renderingText;
        }

        private int width;
        private boolean dirty = true;

        private final int getWidth() {
            final int width;

            if (this.dirty) {
                width = this.width = MinecraftClient.getInstance().textRenderer.getWidth(renderingText.text);
                this.dirty = false;
            } else {
                width = this.width;
            }

            return width;
        }

        private final void markDirty() {
            this.dirty = true;
        }
    }

    @NotNull
    public static final RenderingText createRenderingText() {
        return new RenderingText();
    }

    @NotNull
    public static final RenderingText createRenderingText(@NotNull final String initial) {
        return new RenderingText(initial);
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

    public static final void renderText(@NotNull final DrawContext context, @NotNull final RenderingText text, final int x, final int y, @NotNull final Formatting color) {
        RenderUtils.renderText(context, text, x, () -> y, color);
    }

    public static final void renderText(@NotNull final DrawContext context, @NotNull final RenderingText text, final int x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        RenderUtils.renderText(context, text, () -> x, y, color);
    }

    private static final void renderText(@NotNull final DrawContext context, @NotNull final RenderingText text, final IntSupplier x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        context.drawText(MinecraftClient.getInstance().textRenderer, text.orderedText, x.getAsInt(), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final int middleAlignedXForText(@NotNull final RenderingText text) {
        return (MinecraftClient.getInstance().getWindow().getScaledWidth() >> 1) - (text.widthHolder.getWidth() >> 1);
    }

    public static final void renderCenteredText(@NotNull final DrawContext context, @NotNull final RenderingText text, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        context.drawText(MinecraftClient.getInstance().textRenderer, text.orderedText, RenderUtils.middleAlignedXForText(text), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
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
        final var matrices = context.matrices();

        if (null == matrices) {
            return;
        }

        final var consumers = context.consumers();

        if (null == consumers) {
            return;
        }

        final var buffer = consumers.getBuffer(RenderUtils.BOX_OUTLINE_LAYER);

        // Convert Formatting to RGBA floats
        final var rgb = RenderUtils.convertFormattingToRGBA(color);

        // Translate block position relative to camera
        matrices.push();

        final var camPos = context.gameRenderer().getCamera().getPos().negate();

        matrices.translate(
                camPos.x,
                camPos.y,
                camPos.z
        );

        final var immutablePos = pos.toImmutable();

        final var red = (rgb >> 16 & 0xFF) / 255.0D;
        final var green = (rgb >> 8 & 0xFF) / 255.0D;
        final var blue = (rgb & 0xFF) / 255.0D;
        final var alpha = 1.0D; // fully opaque

        // Draw outline for a single block using RGBA color
        VertexRendering.drawBox(
                matrices.peek(),
                buffer,
                Box.enclosing(immutablePos, immutablePos),
                (float) red, (float) green, (float) blue, (float) alpha
        );

        matrices.pop();
    }
}
