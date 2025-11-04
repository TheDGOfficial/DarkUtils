package gg.darkutils.utils;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import gg.darkutils.DarkUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.OptionalDouble;
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

    private RenderUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final int getMiddleOfScreenYCoords() {
        final var client = MinecraftClient.getInstance();
        return (client.getWindow().getScaledHeight() >> 1) - (client.textRenderer.fontHeight >> 1);
    }

    private static final int convertFormattingToRGBA(@NotNull final Formatting color) {
        return Objects.requireNonNull(
                TextColor.fromFormatting(color),
                "Formatting must convert to TextColor"
        ).getRgb();
    }

    private static final int convertFormattingToOpaqueColor(@NotNull final Formatting color) {
        // set alpha to 255 otherwise will be invisible
        return 0xFF00_0000 | RenderUtils.convertFormattingToRGBA(color);
    }

    public static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final int x, final int y, @NotNull final Formatting color) {
        RenderUtils.renderText(context, text, x, () -> y, color);
    }

    public static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final int x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        RenderUtils.renderText(context, text, () -> x, y, color);
    }

    private static final void renderText(@NotNull final DrawContext context, @NotNull final String text, final IntSupplier x, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        context.drawText(MinecraftClient.getInstance().textRenderer, Text.of(text).asOrderedText(), x.getAsInt(), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final int middleAlignedXForText(@NotNull final String text) {
        final var client = MinecraftClient.getInstance();
        final var textRenderer = client.textRenderer;

        final var textWidth = textRenderer.getWidth(text);

        return (client.getWindow().getScaledWidth() >> 1) - (textWidth >> 1);
    }

    public static final void renderCenteredText(@NotNull final DrawContext context, @NotNull final String text, @NotNull final IntSupplier y, @NotNull final Formatting color) {
        context.drawText(MinecraftClient.getInstance().textRenderer, Text.of(text).asOrderedText(), RenderUtils.middleAlignedXForText(text), y.getAsInt(), RenderUtils.convertFormattingToOpaqueColor(color), false);
    }

    public static final void renderItem(@NotNull final DrawContext context, @NotNull final Item item, final int x, final int y) {
        RenderUtils.renderItem(context, item, x, () -> y);
    }

    private static final void renderItem(@NotNull final DrawContext context, @NotNull final Item item, final int x, @NotNull final IntSupplier y) {
        RenderUtils.renderItem(context, item, () -> x, y);
    }

    private static final void renderItem(@NotNull final DrawContext context, @NotNull final Item item, final IntSupplier x, @NotNull final IntSupplier y) {
        final var stack = new ItemStack(item);
        context.drawItem(stack, x.getAsInt(), y.getAsInt());
    }

    public static final void drawBlockOutline(@NotNull final WorldRenderContext context,
                                              @NotNull final BlockPos pos,
                                              @NotNull final Formatting color) {
        final var matrices = context.matrixStack();

        if (null == matrices) {
            return;
        }

        final var consumers = context.consumers();

        if (null == consumers) {
            return;
        }

        final var buffer = consumers.getBuffer(RenderUtils.BOX_OUTLINE_LAYER);

        // Convert Formatting → RGBA floats
        final var rgb = RenderUtils.convertFormattingToRGBA(color);

        // Translate block position relative to camera
        matrices.push();

        final var camPos = context.camera().getPos().negate();

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
                matrices,
                buffer,
                Box.enclosing(immutablePos, immutablePos),
                (float) red, (float) green, (float) blue, (float) alpha
        );

        matrices.pop();
    }
}
