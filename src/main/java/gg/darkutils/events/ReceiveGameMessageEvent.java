package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LazyConstants;
import gg.darkutils.utils.chat.BasicColor;
import gg.darkutils.utils.chat.BasicFormatting;
import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.SimpleStyle;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

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
                                      @NotNull Supplier<String> contentSupplier,
                                      @NotNull Supplier<Map<SimpleStyle, Boolean>> hasFormattingCache) implements CancellableEvent {
    @NotNull
    private static final Set<SimpleStyle> ALL_STYLE_COMBINATIONS = Set.copyOf(Stream.concat(
                    // all colors without formatting
                    Stream.of(BasicColor.values()).map(SimpleStyle::colored),
                    // all color + formatting combinations
                    Stream.of(BasicColor.values()).flatMap(color ->
                            Stream.of(BasicFormatting.values())
                                    .map(format -> SimpleStyle.colored(color).also(SimpleStyle.formatted(format)))
                    )
            )
            .toList());

    static {
        ClientReceiveMessageEvents.ALLOW_GAME.register(ReceiveGameMessageEvent::post);
    }

    /**
     * Creates a new {@link ReceiveGameMessageEvent} suitable for triggering the event.
     * A cached {@link CancellationState#ofCached()} will be used with non-canceled state by default.
     *
     * @param message The message.
     */
    public ReceiveGameMessageEvent(@NotNull final Text message) {
        this(CancellationState.ofCached(), message, LazyConstants.lazyConstantOf(message::getString), LazyConstants.lazyConstantOf(() -> LazyConstants.lazyMapOf(ReceiveGameMessageEvent.ALL_STYLE_COMBINATIONS, style -> ChatUtils.hasFormatting(message, style))));
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
     * Checks if the message has the given color.
     *
     * @param color The color.
     * @return Whether the message has the given color or not.
     */
    public final boolean isStyledWith(@NotNull final BasicColor color) {
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
    public final boolean isStyledWith(@NotNull final BasicColor color, @NotNull final BasicFormatting formatting) {
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
     * @return Whether the received (plain) message matches the given search or not.
     */
    public final boolean matches(@NotNull final String search) {
        return search.equals(this.content());
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
