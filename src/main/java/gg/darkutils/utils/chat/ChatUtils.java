package gg.darkutils.utils.chat;

import gg.darkutils.events.SentCommandEvent;
import gg.darkutils.events.SentMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.MathUtils;
import gg.darkutils.utils.RoundingMode;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class ChatUtils {
    /**
     * Represents new line character suitable to be used in chat to switch to a new line.
     * This does not depend on any operating system and thus platform-agnostic (implementation detail in Minecraft chat handling).
     */
    @NotNull
    public static final String NEW_LINE = "\n";

    private static final @NotNull ObjectArrayFIFOQueue<String> sendMessageQueue = new ObjectArrayFIFOQueue<>(1);
    private static long lastSentMessageOrCommandAt;

    private ChatUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final void onMessage(@NotNull final SentMessageEvent event) {
        ChatUtils.lastSentMessageOrCommandAt = System.nanoTime();
    }

    private static final void onCommand(@NotNull final SentCommandEvent event) {
        ChatUtils.lastSentMessageOrCommandAt = System.nanoTime();
    }

    public static final void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ChatUtils::onTick);

        EventRegistry.centralRegistry().addListener(ChatUtils::onMessage);
        EventRegistry.centralRegistry().addListener(ChatUtils::onCommand);
    }

    private static final void onTick(@NotNull final MinecraftClient client) {
        final var player = client.player;

        if (null != player && (0L == ChatUtils.lastSentMessageOrCommandAt || System.nanoTime() - ChatUtils.lastSentMessageOrCommandAt > TimeUnit.MILLISECONDS.toNanos(250L))) {
            final var messageOrCommand = ChatUtils.sendMessageQueue.isEmpty() ? null : ChatUtils.sendMessageQueue.dequeue();
            if (null != messageOrCommand && !messageOrCommand.isEmpty()) {
                player.networkHandler.sendChatMessage(messageOrCommand);
            }
        }
    }

    public static final void queueUserSentMessageOrCommand(@NotNull final String messageOrCommand) {
        ChatUtils.sendMessageQueue.enqueue(messageOrCommand);
    }

    public static final boolean hasFormatting(final Text text, @NotNull final Formatting color, final boolean bold) {
        return !text.asOrderedText().accept((index, style, codePoint) -> style.isBold() != bold ||
                null == style.getColor() || !style.getColor().equals(TextColor.fromFormatting(color)));
    }

    @NotNull
    public static final MutableText gradient(@NotNull final String startHex, @NotNull final String endHex, @NotNull final String text) {
        final var start = ChatUtils.hexToRGB(startHex);
        final var end = ChatUtils.hexToRGB(endHex);

        final var length = text.length();

        var root = Text.literal("");

        for (var i = 0; i < length; ++i) {
            final var t = 1 == length ? 0.0D : i / (length - 1.0D);
            final var color = ChatUtils.interpolate(start, end, t);

            root = root.append(Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(color)));
        }

        return root;
    }

    @NotNull
    public static final Text button(@NotNull final String startHex, @NotNull final String endHex, @NotNull final String label, @NotNull final String hover, @NotNull final String command, final boolean centered, final boolean bold) {
        final var hoverText = ChatUtils.gradient(startHex, endHex, hover);
        hoverText.setStyle(hoverText.getStyle().withBold(bold));

        final var buttonLabel = '[' + label + ']';
        final var text = Text.literal("");

        if (centered) {
            final var fillerSize = ChatUtils.center(buttonLabel, bold).replace(buttonLabel, "").length();
            text.append(" ".repeat(fillerSize));
        }

        final var button = ChatUtils.gradient(startHex, endHex, buttonLabel);
        button.setStyle(
                button.getStyle()
                        .withBold(bold)
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(hoverText))
        );

        text.append(button);

        return text;
    }

    @NotNull
    public static final String center(@NotNull final String text, final boolean bold) {
        final var mc = MinecraftClient.getInstance();

        final var width = mc.textRenderer.getWidth(Text.literal(text).setStyle(Style.EMPTY.withBold(bold)));
        final var halvedWidth = width >> 1;

        final var toCompensate = (mc.inGameHud.getChatHud().getWidth() >> 1) - halvedWidth;
        final var fillerWidth = mc.textRenderer.getWidth(Text.literal(" ").setStyle(Style.EMPTY.withBold(bold))) + 1;

        final var builder = new StringBuilder(text.length());

        var compensated = 0;

        while (compensated < toCompensate) {
            builder.append(' ');
            compensated += fillerWidth;
        }

        return builder + text;
    }

    @NotNull
    public static final String fill(final char character, final boolean bold) {
        return ChatUtils.fillRemainingOf(character, bold, "");
    }

    @NotNull
    public static final String fillRemainingOf(final char character, final boolean bold, @NotNull final String middleText) {
        final var mc = MinecraftClient.getInstance();
        final var style = Style.EMPTY.withBold(bold);

        final var chatWidth = mc.inGameHud.getChatHud().getWidth();
        final var middleWidth = mc.textRenderer.getWidth(Text.literal(middleText).setStyle(style));
        final var toCompensate = chatWidth - middleWidth;

        // If text is too wide, just return the middle text unchanged
        if (0 >= toCompensate) {
            return middleText;
        }

        final var fillerStr = Character.toString(character);
        final var fillerWidth = mc.textRenderer.getWidth(Text.literal(fillerStr).setStyle(style));
        if (0 >= fillerWidth) {
            return middleText;
        }

        // Estimate total filler count
        final var totalFillers = Math.max(0, toCompensate / fillerWidth);

        // Split between left and right
        final var leftFillers = totalFillers >> 1;

        // Build base candidate
        final var rightFillers = totalFillers - leftFillers;
        var best = String.valueOf(character).repeat(leftFillers) +
                middleText +
                String.valueOf(character).repeat(Math.max(0, rightFillers));
        final var bestWidth = mc.textRenderer.getWidth(Text.literal(best).setStyle(style));
        var bestDiff = Math.abs(chatWidth - bestWidth);

        // --- Adjustment Phase ---
        // Try shifting one filler left/right or adding/removing one on either side
        // until we can’t improve the fit
        for (var adjust = 0; 16 > adjust; ++adjust) {
            // Try adding one to the right
            var test = best + character;
            var testWidth = mc.textRenderer.getWidth(Text.literal(test).setStyle(style));
            var diff = Math.abs(chatWidth - testWidth);
            if (diff < bestDiff) {
                best = test;
                bestDiff = diff;
                continue;
            }

            // Try adding one to the left
            test = character + best;
            testWidth = mc.textRenderer.getWidth(Text.literal(test).setStyle(style));
            diff = Math.abs(chatWidth - testWidth);
            if (diff < bestDiff) {
                best = test;
                bestDiff = diff;
                continue;
            }

            // No improvement possible → stop
            return best;
        }

        return best;
    }

    public static final int hexToRGB(@NotNull final String hex) {
        var hexAsLong = hex;

        if (!hexAsLong.isEmpty() && '#' == hexAsLong.charAt(0)) {
            hexAsLong = hexAsLong.substring(1);
        }

        return Integer.parseInt(hexAsLong, 16);
    }

    private static final int interpolate(final int start, final int end, final double progress) {
        final var redStart = start >> 16 & 0xFF;
        final var greenStart = start >> 8 & 0xFF;
        final var blueStart = start & 0xFF;
        final var redEnd = end >> 16 & 0xFF;
        final var greenEnd = end >> 8 & 0xFF;
        final var blueEnd = end & 0xFF;

        // We want the non-decimal part of number to stay the same if decimal part is half
        final var roundingMode = RoundingMode.HALF_DOWN;

        final var red = MathUtils.round(redStart + (redEnd - redStart) * progress, roundingMode);
        final var green = MathUtils.round(greenStart + (greenEnd - greenStart) * progress, roundingMode);
        final var blue = MathUtils.round(blueStart + (blueEnd - blueStart) * progress, roundingMode);

        return red << 16 | green << 8 | blue;
    }
}
