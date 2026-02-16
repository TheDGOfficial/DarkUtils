package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.utils.LazyConstants;
import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.SimpleColor;
import gg.darkutils.utils.chat.SimpleFormatting;
import gg.darkutils.utils.chat.SimpleStyle;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Triggers when a game message in chat was received by the local client from the server.
 * <p>
 * The server can be an integrated or dedicated server, or a realm.
 * <p>
 * Cancelling will make the local client not display the message in chat.
 *
 * @param cancellationState The cancellation state holder.
 * @param message           The message.
 */
public record ReceiveGameMessageEvent(@NotNull CancellationState cancellationState,
                                      @NotNull Text message,
                                      @NotNull Supplier<String> rawContentSupplier,
                                      @NotNull Supplier<String> contentSupplier,
                                      @NotNull Supplier<Map<SimpleStyle, Boolean>> hasFormattingCache) implements CancellableEvent {
    @NotNull
    private static final Set<SimpleStyle> ALL_STYLE_COMBINATIONS = Set.copyOf(Stream.concat(
                    // all colors without formatting
                    Stream.of(SimpleColor.values()).map(SimpleStyle::colored),
                    // all color + formatting combinations
                    Stream.of(SimpleColor.values()).flatMap(color ->
                            Stream.of(SimpleFormatting.values())
                                    .map(format -> SimpleStyle.colored(color).also(SimpleStyle.formatted(format)))
                    )
            )
            .toList());

    /**
     * Creates a new {@link ReceiveGameMessageEvent} suitable for triggering the event.
     * A fresh {@link CancellationState#ofFresh()} will be used with non-canceled state by default.
     *
     * @param cancellationState  The cancellation state holder.
     * @param message            The message.
     * @param rawContentSupplier The raw message.
     * @param contentSupplier    The content supplier.
     */
    public ReceiveGameMessageEvent(@NotNull final CancellationState cancellationState, @NotNull final Text message, @NotNull final Supplier<String> rawContentSupplier, @NotNull final Supplier<String> contentSupplier) {
        this(cancellationState, message, rawContentSupplier, contentSupplier, LazyConstants.lazyConstantOf(() -> LazyConstants.lazyMapOf(ReceiveGameMessageEvent.ALL_STYLE_COMBINATIONS, style -> ChatUtils.hasFormatting(message, style, rawContentSupplier))));
    }

    /**
     * Creates a new {@link ReceiveGameMessageEvent} suitable for triggering the event.
     * A fresh {@link CancellationState#ofFresh()} will be used with non-canceled state by default.
     *
     * @param cancellationState  The cancellation state holder.
     * @param message            The message.
     * @param rawContentSupplier The raw message.
     */
    public ReceiveGameMessageEvent(@NotNull final CancellationState cancellationState, @NotNull final Text message, @NotNull final Supplier<String> rawContentSupplier) {
        this(cancellationState, message, rawContentSupplier, LazyConstants.lazyConstantOf(() -> ChatUtils.removeControlCodes(rawContentSupplier.get())));
    }

    /**
     * Creates a new {@link ReceiveGameMessageEvent} suitable for triggering the event.
     * A fresh {@link CancellationState#ofFresh()} will be used with non-canceled state by default.
     *
     * @param message The message.
     */
    public ReceiveGameMessageEvent(@NotNull final Text message) {
        this(CancellationState.ofFresh(), message, LazyConstants.lazyConstantOf(message::getString));
    }

    public static final void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(ReceiveGameMessageEvent::post);
    }

    private static final boolean post(@NotNull final Text message, final boolean overlay) {
        return overlay || new ReceiveGameMessageEvent(message).triggerAndNotCancelled();
    }

    /**
     * Returns the cached (plain) content of the message.
     *
     * @return The cached (plain) content of the message.
     */
    @NotNull
    public final String content() {
        return this.contentSupplier.get();
    }

    /**
     * Returns the cached raw content of the message.
     * For legacy formatting, this might have color characters.
     *
     * @return The cached raw content of the message.
     */
    @NotNull
    public final String rawContent() {
        return this.rawContentSupplier.get();
    }

    /**
     * Checks if the message has the given color.
     *
     * @param color The color.
     * @return Whether the message has the given color or not.
     */
    public final boolean isStyledWith(@NotNull final SimpleColor color) {
        final var style = SimpleStyle.colored(color);

        return this.isStyledWith(style);
    }

    /**
     * Checks if the message has the given color and formatting.
     *
     * @param color      The color.
     * @param formatting The formatting.
     * @return Whether the message has the given color and formatting.
     */
    public final boolean isStyledWith(@NotNull final SimpleColor color, @NotNull final SimpleFormatting formatting) {
        final var style = SimpleStyle.colored(color).also(SimpleStyle.formatted(formatting));

        return this.isStyledWith(style);
    }

    /**
     * Checks if the message has the given style.
     * <p>
     * Private API, use other overloads that accept a color or a color and formatting instead.
     * <p>
     * This ensures no other unrelated styles such as centered or inherited are exposed to the API.
     *
     * @param style The style.
     * @return Whether the message has the given style.
     */
    private final boolean isStyledWith(@NotNull final SimpleStyle style) {
        return this.hasFormattingCache.get().get(style);
    }

    /**
     * Checks if the received (plain) message matches the given search.
     * <p>
     * This is equivalent to doing:
     * {@snippet :
     * "search".equals(event.content())
     * event.matches("search") // same result
     *}
     *
     * @param search The string that is being earched for exact equality.
     * @return Whether the received (plain) message matches the given search or not.
     */
    public final boolean matches(@NotNull final String search) {
        return search.equals(this.content());
    }

    /**
     * Extracts a substring from the message, strictly starting after the given {@code strictStart}
     * (exclusive) and ending before the first occurrence of the specified {@code end} delimiter
     * (exclusive).
     *
     * <p>This method requires the message to start with {@code strictStart}. The {@code end}
     * delimiter must appear after the prefix.</p>
     *
     * <p>This implementation is optimized for strict prefix-based extraction and is the
     * fastest approach for this specific use case.</p>
     *
     * @param strictStart the required starting prefix (must match at index 0)
     * @param end the delimiter marking the end of the extracted section
     * @return the extracted substring, or {@code null} if the message does not start with
     *         {@code strictStart} or if the {@code end} delimiter is not found after it
     */
    @Nullable
    public final String extractPart(@NotNull final String strictStart, @NotNull final char end) {
        final var content = this.content();

        if (!content.startsWith(strictStart)) {
            return null;
        }

        final int from = strictStart.length();
        final int to = content.indexOf(end, from);

        return -1 == to ? null : content.substring(from, to);
    }

    /**
     * Runs an action based on what the received (plain) message is.
     * For the action to match and run, the (plain) message has to match exactly one of the given cases.
     * <p>
     * Other than the cost of map creation, performance should be similar to using a switch.
     * Recommended to save and re-use the map to a static variable if best performance is needed.
     *
     * @param matchCase The map of messages and what action to run, accepting the event as parameter.
     */
    public final void match(@NotNull final Map<String, Consumer<ReceiveGameMessageEvent>> matchCase) {
        final var content = this.content();
        final var result = matchCase.get(content);

        if (null != result) {
            result.accept(this);
        }
    }
}
