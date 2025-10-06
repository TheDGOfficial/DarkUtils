package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.BasicEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Listener for an {@link Event}.
 * <p>
 * Single abstract method accept(T) keeps this a functional interface while allowing
 * default methods for priority and receiveCancelled behaviour.
 * <p>
 * See {@link EventListener#create(EventListener, EventPriority, boolean)} to create a
 * listener with custom priority or receiveCancelled behaviour.
 *
 * @param <T> The type of the event the listener is listening for.
 */
@FunctionalInterface
public interface EventListener<T extends Event> extends Consumer<T> {
    /**
     * Creates an event listener with custom priority and receiveCancelled behaviour.
     *
     * @param listener         The basic event listener.
     * @param priority         The custom event priority.
     * @param receiveCancelled The custom behaviour on whether to accept cancelled events.
     * @param <T>              The type of the event we are listening for.
     * @return The new event listener with custom priority and receiveCancelled set that delegates
     * to the passed basic event listener.
     */
    @NotNull
    static <T extends Event> EventListener<T> create(@NotNull final EventListener<T> listener, @NotNull final EventPriority priority, final boolean receiveCancelled) {
        return new BasicEventListener<>(listener, priority, receiveCancelled);
    }

    /**
     * Triggers this event listener.
     *
     * @param event The event.
     */
    void onEvent(@NotNull final T event);

    /**
     * Triggers this event listener.
     * <p>
     * Use instead {@link EventListener#onEvent(Event)} for clarity unless
     * compatibility with an API that expects {@link Consumer} is required.
     * <p>
     * As the {@link Consumer#accept(Object)} allows null parameters, a runtime
     * check will be performed to ensure the given event is not null.
     *
     * @param event
     */
    @Override
    default void accept(@Nullable final T event) {
        Objects.requireNonNull(event, "event");

        this.onEvent(event);
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
     * Whether this listener should still receive events that are already cancelled.
     * Default false.
     *
     * @return Whether this listener should still receive events that are already cancelled.
     */
    default boolean receiveCancelled() {
        return false;
    }
}
