package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Declares an {@link EventHandler}.
 * <p>
 * If two {@link EventListener}s have the same priority, implementors must
 * ensure registration order.
 *
 * @param <T> The type of the event this {@link EventHandler} is for.
 */
public interface EventHandler<T extends Event> {
    /**
     * Gets a sorted, unmodifiable list of active listeners listening to the event this {@link EventHandler} is handling.
     *
     * @return A sorted, unmodifiable list of active listeners listening to the event.
     */
    @NotNull
    Iterable<EventListener<? super Event>> getListeners();

    /**
     * Adds an {@link EventListener} to listen for the event this handler is handling.
     * The listener list will be copied for thread-safety and sorted by {@link EventPriority}
     * as part of this modification to the list of listeners.
     * <p>
     * If the listener is already registered, an {@link IllegalStateException} will be thrown.
     *
     * @param listener An {@link EventListener} to add to the list of listeners.
     */
    void addListener(final @NotNull EventListener<? super Event> listener);

    /**
     * Adds an {@link EventListener} to listen for the event this handler is handling.
     * The listener list will be copied for thread-safety and sorted by {@link EventPriority}
     * as part of this modification to the list of listeners.
     * <p>
     * If the listener is already registered, an {@link IllegalStateException} will be thrown.
     *
     * @param listener An {@link EventListener} to add to the list of listeners.
     * @return The created event listener, which might be passed to {@link EventHandler#removeListener(EventListener)}.
     */
    @NotNull
    default EventListener<? super Event> addListener(final @NotNull Consumer<? super Event> listener) {
        return this.addListener(listener, EventPriority.NORMAL, false);
    }

    /**
     * Adds an {@link EventListener} to listen for the event this handler is handling.
     * The listener list will be copied for thread-safety and sorted by {@link EventPriority}
     * as part of this modification to the list of listeners.
     * <p>
     * If the listener is not a registered listener or already removed, an {@link IllegalStateException} will be
     * thrown.
     *
     * @param listener         An {@link EventListener} to add to the list of listeners.
     * @param priority         The priority of the {@link EventListener}, determining the order it will run,
     *                         and the impact of cancellation affecting other {@link EventListener}s.
     * @param receiveCancelled Whether this {@link EventListener} should receive canceled events or not.
     *                         Can be used to uncancel canceled events, allowing other {@link EventListener}s
     *                         coming after to run and the event to happen.
     * @return The created event listener, which might be passed to {@link EventHandler#removeListener(EventListener)}.
     */
    @NotNull
    default EventListener<? super Event> addListener(final @NotNull Consumer<? super Event> listener, final @NotNull EventPriority priority, final boolean receiveCancelled) {
        final var eventListener = EventListener.create(listener, priority, receiveCancelled);
        this.addListener(eventListener);

        return eventListener;
    }

    /**
     * Removes an {@link EventListener} from listening for the event this handler is handling.
     * The listener list will be copied for thread-safety as part of this modification to the list of listeners.
     *
     * @param listener An {@link EventListener} to remove from the list of listeners.
     */
    void removeListener(final @NotNull EventListener<T> listener);

    /**
     * Triggers a cancellable event, calling all listeners {@link EventListener#accept(Object)} sequentially in the calling thread,
     * ordered based on {@link EventPriority}.
     * <p>
     * If any {@link EventListener} cancels the event, the further event listeners
     * won't be called unless they return true from {@link EventListener#receiveCancelled()}.
     * <p>
     * If an event is canceled by an {@link EventListener} and then uncanceled at a later point by an event receiving canceled
     * events due to {@link EventListener#receiveCancelled()}, the further {@link EventListener}s will receive the event even if
     * they return false from {@link EventListener#receiveCancelled()}.
     * <p>
     * Implementors must ensure that even if any of the listeners at any point throw any exceptions, it should not stop the further
     * {@link EventListener}s from receiving the event.
     *
     * @param event The event to trigger.
     * @return The {@link CancellationResult} of the event after going through all listeners mutations to the state.
     */
    @NotNull
    <E extends Event & CancellableEvent> CancellationResult triggerCancellableEvent(final @NotNull E event);

    /**
     * Triggers a non-cancellable event, calling all listeners {@link EventListener#accept(Object)} sequentially in the calling thread,
     * ordered based on {@link EventPriority}.
     * <p>
     * Implementors must ensure that even if any of the listeners at any point throw any exceptions, it should not stop the further
     * {@link EventListener}s from receiving the event.
     *
     * @param event The event to trigger.
     */
    <E extends Event & NonCancellableEvent> void triggerNonCancellableEvent(final @NotNull E event);
}
