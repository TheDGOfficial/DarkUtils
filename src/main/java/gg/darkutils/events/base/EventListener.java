package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Listener for an {@link Event}.
 * <p>
 * Single abstract method onEvent(T) keeps this a functional interface while allowing
 * default methods for priority and receiveCancelled behavior.
 * <p>
 * See {@link EventListener#create(Consumer, EventPriority, boolean)} to create a
 * listener with custom priority or receiveCancelled behavior.
 *
 * @param <T> The type of the event the listener is listening for.
 */
public sealed interface EventListener<T extends Event> extends Consumer<T> permits EventListener.Impl {
    /**
     * Creates an event listener with custom priority and receiveCancelled behavior.
     *
     * @param listener         The basic event listener.
     * @param priority         The custom event priority.
     * @param receiveCancelled The custom behavior on whether to accept canceled events.
     * @param <T>              The type of the event we are listening for.
     * @return The new event listener with custom priority and receiveCancelled set that delegates
     * to the passed basic event listener.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    static <T extends Event> EventListener<T> create(@NotNull final Consumer<? super T> listener, @NotNull final EventPriority priority, final boolean receiveCancelled) {
        return new EventListener.Impl<>((Consumer<T>) listener, priority, receiveCancelled);
    }

    /**
     * Priority of this listener. Higher priorities are invoked earlier.
     * Default is NORMAL.
     *
     * @return The priority of this listener.
     */
    @NotNull
    default EventPriority priority() {
        return EventPriority.NORMAL;
    }

    /**
     * Whether this listener should still receive events that are already canceled.
     * Default false.
     *
     * @return Whether this listener should still receive events that are already canceled.
     */
    default boolean receiveCancelled() {
        return false;
    }

    /**
     * An event listener implementation that delegates to a consumer with custom priority and receiveCancelled behavior.
     *
     * @param listener         The base listener to be used as a delegate.
     * @param priority         The custom {@link EventPriority} priority.
     * @param receiveCancelled The custom receiveCancelled behavior.
     * @param <T>              The type of the event.
     */
    public record Impl<T extends Event>(@NotNull Consumer<T> listener, @NotNull EventPriority priority,
                                        boolean receiveCancelled) implements EventListener<T> {
        @Override
        public final void accept(@NotNull final T event) {
            this.listener.accept(event);
        }
    }
}

