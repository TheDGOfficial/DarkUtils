package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

/**
 * Listener for an {@link Event}.
 * <p>
 * Single abstract method onEvent(T) keeps this a functional interface while allowing
 * default methods for priority and receiveCancelled behavior.
 * <p>
 * See {@link EventListener#create(EventConsumer, EventPriority, boolean)} to create a
 * listener with custom priority or receiveCancelled behavior.
 *
 * @param <T> The type of the event the listener is listening for.
 */
public sealed interface EventListener<T extends Event> permits EventListener.Impl {
    /**
     * Creates an event listener.
     *
     * @param listener The basic event listener (consumer).
     * @param <T>      The type of the event we are listening for.
     * @return The new event listener that delegates to the passed consumer.
     */
    @NotNull
    static <T extends Event> EventListener<T> create(@NotNull final EventConsumer<? super T> listener) {
        return EventListener.create(listener, EventPriority.NORMAL);
    }

    /**
     * Creates an event listener with custom priority.
     *
     * @param listener The basic event listener (consumer).
     * @param priority The custom event priority.
     * @param <T>      The type of the event we are listening for.
     * @return The new event listener with custom priority set that delegates
     * to the passed consumer.
     */
    @NotNull
    static <T extends Event> EventListener<T> create(@NotNull final EventConsumer<? super T> listener, @NotNull final EventPriority priority) {
        return EventListener.create(listener, priority, false);
    }

    /**
     * Creates an event listener with custom priority and receiveCancelled behavior.
     *
     * @param listener         The basic event listener (consumer).
     * @param priority         The custom event priority.
     * @param receiveCancelled The custom behavior on whether to accept canceled events.
     * @param <T>              The type of the event we are listening for.
     * @return The new event listener with custom priority and receiveCancelled set that delegates
     * to the passed consumer.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    static <T extends Event> EventListener<T> create(@NotNull final EventConsumer<? super T> listener, @NotNull final EventPriority priority, final boolean receiveCancelled) {
        return new EventListener.Impl<>((EventConsumer<T>) listener, priority, receiveCancelled);
    }

    /**
     * Priority of this listener. Higher priorities are invoked earlier.
     * Default is NORMAL.
     *
     * @return The priority of this listener.
     */
    @NotNull
    EventPriority priority();

    /**
     * Whether this listener should still receive events that are already canceled.
     * Default false.
     *
     * @return Whether this listener should still receive events that are already canceled.
     */
    boolean receiveCancelled();

    /**
     * Runs this listener.
     *
     * @param event The event.
     */
    void onEvent(@NotNull final T event);

    /**
     * An event listener implementation that delegates to a consumer with custom priority and receiveCancelled behavior.
     *
     * @param listener         The base listener to be used as a delegate.
     * @param priority         The custom {@link EventPriority} priority.
     * @param receiveCancelled The custom receiveCancelled behavior.
     * @param <T>              The type of the event.
     */
    public record Impl<T extends Event>(@NotNull EventConsumer<T> listener, @NotNull EventPriority priority,
                                        boolean receiveCancelled) implements EventListener<T> {
        @Override
        public final void onEvent(@NotNull final T event) {
            this.listener.onEvent(event);
        }
    }
}

