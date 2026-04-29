package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

/**
 * Listener without priority or receiveCancelled for an {@link Event}.
 * <p>
 * Single abstract method onEvent(T) triggered when event triggers.
 * <p>
 * See {@link EventListener#create(EventConsumer, EventPriority, boolean)} to create a
 * listener with custom priority or receiveCancelled behavior.
 *
 * @param <T> The type of the event the listener is listening for.
 */
@FunctionalInterface
public interface EventConsumer<T extends Event> {
    /**
     * Runs this listener.
     *
     * @param event The event.
     */
    void onEvent(@NotNull final T event);
}
