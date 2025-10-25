package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers after a screen has been opened but not yet displayed to the user.
 * <p>
 * Cancelling will make the screen act as if it was never opened, including
 * sending an automatic close packet.
 *
 * @param cancellationState The cancellation state holder.
 * @param screenHandlerType Screen handler type.
 * @param name              Name of the screen.
 */
public record OpenScreenEvent(@NotNull CancellationState cancellationState,
                              @NotNull ScreenHandlerType<?> screenHandlerType,
                              @NotNull Text name) implements CancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(OpenScreenEvent.class);
    }

    /**
     * Creates a new {@link OpenScreenEvent} suitable for triggering the event.
     * A cached {@link CancellationState#ofCached()} will be used with non-cancelled state by default.
     *
     * @param screenHandlerType The screen handler type.
     * @param name              The name of the screen.
     */
    public OpenScreenEvent(@NotNull final ScreenHandlerType<?> screenHandlerType, @NotNull final Text name) {
        this(CancellationState.ofCached(), screenHandlerType, name);
    }
}
