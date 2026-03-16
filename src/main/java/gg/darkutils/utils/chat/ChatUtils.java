package gg.darkutils.utils.chat;

import gg.darkutils.mixin.accessors.ChatHudAccessor;
import gg.darkutils.events.SentCommandEvent;
import gg.darkutils.events.SentMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.MathUtils;
import gg.darkutils.utils.RoundingMode;
import gg.darkutils.utils.TickUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ChatUtils {
    /**
     * Represents new line character suitable to be used in chat to switch to a new line.
     * This does not depend on any operating system and thus platform-agnostic (implementation detail in Minecraft chat handling).
     */
    @NotNull
    public static final String NEW_LINE = "\n";
    /**
     * The character used to signal the start of a formatting code.
     * <p>
     * We store this as a {@link String} since that has higher performance
     * than an unboxed {@link Character}, although primitive char is more
     * performant, we need the {@link Character} for string operations.
     */
    @NotNull
    private static final String CONTROL_START = "§";
    private static final long QUARTER_SECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(250L);

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
        if (null == player || ChatUtils.sendMessageQueue.isEmpty()) {
            return;
        }

        final var lastSentAt = ChatUtils.lastSentMessageOrCommandAt;
        if (0L != lastSentAt &&
                System.nanoTime() - lastSentAt <= ChatUtils.QUARTER_SECOND_NANOS) {
            return;
        }

        final var messageOrCommand = ChatUtils.sendMessageQueue.dequeue();
        if (null != messageOrCommand && !messageOrCommand.isEmpty()) {
            player.networkHandler.sendChatMessage(messageOrCommand);
        }
    }

    public static final void addToSendMessageQueue(@NotNull final String message) {
        ChatUtils.sendMessageQueue.enqueue(message);
    }

    public static final void sendMessageToLocalPlayer(@NotNull final Text text) {
        // Ensure player is available and no lost messages
        // The awaitLocalPlayer method ensures correct threading behavior internally.
        TickUtils.awaitLocalPlayer(player -> player.sendMessage(text, false));
    }

    public static final boolean hasFormatting(@NotNull final Text text, @NotNull final SimpleColor color) {
        return ChatUtils.hasFormatting(text, SimpleStyle.colored(color));
    }

    public static final boolean hasFormatting(@NotNull final Text text, @NotNull final SimpleColor color, @NotNull final SimpleFormatting formatting) {
        return ChatUtils.hasFormatting(text, SimpleStyle.colored(color).also(SimpleStyle.formatted(formatting)));
    }

    public static final boolean hasFormatting(@NotNull final Text text, @NotNull final SimpleStyle style) {
        return ChatUtils.hasFormatting(text, style, text::getString);
    }

    public static final boolean hasFormatting(@NotNull final Text text, @NotNull final SimpleStyle style, @NotNull final Supplier<String> textString) {
        final var hasFormattingInComponent = ChatUtils.hasFormattingInsideRootComponent(text, style);

        return hasFormattingInComponent || ChatUtils.hasFormattingInsideTextRaw(textString.get(), style);
    }

    private static final boolean hasFormattingInsideTextRaw(@NotNull final String rawText, @NotNull final SimpleStyle style) {
        return rawText.contains(style.getRawFormattingCharacters());
    }

    private static final boolean hasFormattingInsideRootComponent(@NotNull final Text text, @NotNull final SimpleStyle simpleStyle) {
        return ChatUtils.hasFormattingInsideRootComponent(text, simpleStyle.toStyle()); // converts from our SimpleStyle wrapper to vanilla Style class
    }

    private static final boolean hasFormattingInsideRootComponent(@NotNull final Text text, @NotNull final Style expected) {
        return text.visit((resolved, ignored) ->
                        ChatUtils.matches(resolved, expected)
                                ? Optional.of(true)
                                : Optional.empty(),
                Style.EMPTY
        ).isPresent();
    }

    private static final boolean matches(@NotNull final Style resolved, @NotNull final Style target) {
        return ChatUtils.matchNullable(target.getColor(), resolved.getColor()) && ChatUtils.matchFlag(target.isBold(), resolved.isBold()) && ChatUtils.matchFlag(target.isItalic(), resolved.isItalic()) && ChatUtils.matchFlag(target.isUnderlined(), resolved.isUnderlined()) && ChatUtils.matchFlag(target.isStrikethrough(), resolved.isStrikethrough()) && ChatUtils.matchFlag(target.isObfuscated(), resolved.isObfuscated()) && ChatUtils.matchNullable(target.getClickEvent(), resolved.getClickEvent()) && ChatUtils.matchNullable(target.getHoverEvent(), resolved.getHoverEvent()) && ChatUtils.matchNullable(target.getInsertion(), resolved.getInsertion()) && ChatUtils.matchNullable(target.getFont(), resolved.getFont()) && ChatUtils.matchNullable(target.getShadowColor(), resolved.getShadowColor());
    }

    private static final boolean matchFlag(final boolean target, final boolean resolved) {
        return !target || resolved;
    }

    private static final <T> boolean matchNullable(@Nullable final T target, @Nullable final T resolved) {
        return null == target || target.equals(resolved);
    }

    /**
     * Removes Minecraft color and formatting codes from the given string.
     * <p>
     * Note that unlike other methods this doesn't utilize a
     * {@link java.util.regex.Pattern} or {@link java.util.regex.Matcher} and
     * just uses simple {@link StringBuilder} and thus will be much faster.
     *
     * @param text The text to remove Minecraft control codes from.
     * @return The given text without control codes.
     */
    @NotNull
    public static final String removeControlCodes(@NotNull final String text) {
        final var length = text.length();

        if (0 == length) {
            return "";
        }

        var nextFormattingSequence = text.indexOf(ChatUtils.CONTROL_START);

        if (-1 == nextFormattingSequence) {
            return text;
        }

        final var cleanedString = new StringBuilder(length - 1);

        var readIndex = 0;

        while (-1 != nextFormattingSequence) {
            cleanedString.append(text, readIndex, nextFormattingSequence);

            readIndex = nextFormattingSequence + 2;
            nextFormattingSequence = text.indexOf(ChatUtils.CONTROL_START, readIndex);

            readIndex = Math.min(length, readIndex);
        }

        cleanedString.append(text, readIndex, length);

        return cleanedString.toString();
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

    private static final int getChatWidth(@NotNull final MinecraftClient mc) {
        return ((ChatHudAccessor) mc.inGameHud.getChatHud()).callGetWidth();
    }

    @NotNull
    public static final String center(@NotNull final String text, final boolean bold) {
        final var mc = MinecraftClient.getInstance();

        final var width = mc.textRenderer.getWidth(Text.literal(text).setStyle(Style.EMPTY.withBold(bold)));
        final var halvedWidth = width >> 1;

        final var toCompensate = (ChatUtils.getChatWidth(mc) >> 1) - halvedWidth;
        final var fillerWidth = mc.textRenderer.getWidth(Text.literal(" ").setStyle(Style.EMPTY.withBold(bold))) + 1;

        final var builder = new StringBuilder(text.length());

        for (var compensated = 0; compensated < toCompensate; compensated += fillerWidth) {
            builder.append(' ');
        }

        return builder + text;
    }

    @NotNull
    public static final String fill(final char character, final boolean bold) {
        return ChatUtils.fillRemainingOf(character, bold, "");
    }

    private static final void validateAvailable(@NotNull final String name, @Nullable final Object nullable) {
        if (null == nullable) {
            throw new IllegalStateException("called before " + name + " is available");
        }
    }

    private static final int getChatWidth(@NotNull final MinecraftClient mc) {
        final var inGameHud = mc.inGameHud;

        ChatUtils.validateAvailable("inGameHud", inGameHud);

        return inGameHud.getChatHud().getWidth();
    }

    @NotNull
    public static final String fillRemainingOf(final char character, final boolean bold, @NotNull final String middleText) {
        final var mc = MinecraftClient.getInstance();
        final var style = Style.EMPTY.withBold(bold);

        final var chatWidth = ChatUtils.getChatWidth(mc);
        final var textRenderer = mc.textRenderer;

        ChatUtils.validateAvailable("textRenderer", textRenderer);
        ChatUtils.validateAvailable("player", mc.player);

        final var middleWidth = textRenderer.getWidth(Text.literal(middleText).setStyle(style));
        final var toCompensate = chatWidth - middleWidth;

        // If text is too wide, just return the middle text unchanged
        if (0 >= toCompensate) {
            return middleText;
        }

        final var characterStr = Character.toString(character);
        final var fillerWidth = textRenderer.getWidth(Text.literal(characterStr).setStyle(style));

        if (0 >= fillerWidth) {
            return middleText;
        }

        // Estimate total filler count
        final var totalFillers = Math.max(0, toCompensate / fillerWidth);

        // Split between left and right
        final var leftFillers = totalFillers >> 1;

        // Build base candidate
        final var rightFillers = totalFillers - leftFillers;

        var best = characterStr.repeat(leftFillers) + middleText + characterStr.repeat(rightFillers);
        var bestDiff = Math.abs(chatWidth - textRenderer.getWidth(Text.literal(best).setStyle(style)));

        // --- Adjustment Phase ---
        // Try shifting one filler left/right or adding/removing one on either side
        // until we can’t improve the fit
        for (var adjust = 0; 16 > adjust; ++adjust) {
            // Try adding one to the right
            var test = best + character;
            var diff = Math.abs(chatWidth - textRenderer.getWidth(Text.literal(test).setStyle(style)));
            if (diff < bestDiff) {
                best = test;
                bestDiff = diff;
                continue;
            }

            // Try adding one to the left
            test = character + best;
            diff = Math.abs(chatWidth - textRenderer.getWidth(Text.literal(test).setStyle(style)));
            if (diff < bestDiff) {
                best = test;
                bestDiff = diff;
                continue;
            }

            // No improvement possible - stop
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
