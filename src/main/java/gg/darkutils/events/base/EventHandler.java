package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.BasicEventHandler;
import org.jetbrains.annotations.NotNull;

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
     * Gets a sorted, unmodifiable list of active listeners listening to the event this {@link BasicEventHandler} is handling.
     *
     * @return A sorted, unmodifiable list of active listeners listening to the event.
     */
    @NotNull
    Iterable<EventListener<T>> getListeners();

    /**
     * Adds an {@link EventListener} to listen for the event this handler is handling.
     * The listener list will be copied for thread-safety and sorted by {@link EventPriority}
     * as part of this modification to the list of listeners.
     * <p>
     * If the listener is already registered, an {@link IllegalStateException} will be thrown.
     *
     * @param listener An {@link EventListener} to add to the list of listeners.
     */
    void addListener(final @NotNull EventListener<T> listener);

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
     * @param receiveCancelled Whether this {@link EventListener} should receive cancelled events or not.
     *                         Can be used to uncancel cancelled events, allowing other {@link EventListener}s
     *                         coming after to run and the event to happen.
     */
    default void addListener(final @NotNull EventListener<T> listener, final @NotNull EventPriority priority, final boolean receiveCancelled) {
        this.addListener(EventListener.create(listener, priority, receiveCancelled));
    }

    /**
     * Removes an {@link EventListener} from listening for the event this handler is handling.
     * The listener list will be copied for thread-safety and sorted by {@link EventPriority}
     * as part of this modification to the list of listeners.
     *
     * @param listener An {@link EventListener} to remove from the list of listeners.
     */
    void removeListener(final @NotNull EventListener<T> listener);

    /**
     * Triggers an event, calling all listeners {@link EventListener#accept(Event)} sequentially in the calling thread,
     * ordered based on {@link EventPriority}.
     * <p>
     * If the event is {@link CancellableEvent} and any {@link EventListener} cancels the event, the further event listeners
     * won't be called unless they return true from {@link EventListener#receiveCancelled()}.
     * <p>
     * If an event is cancelled by an {@link EventListener} and then uncancelled at a later point by an event receiving cancelled
     * events due to {@link EventListener#receiveCancelled()}, the further {@link EventListener}s will receive the event even if
     * they return false from {@link EventListener#receiveCancelled()}.
     * <p>
     * Implementors must ensure that even if any of the listeners at any point throw any exceptions, it should not stop the further
     * {@link EventListener}s from receiving the event.
     *
     * @param event The event to trigger.
     * @return The {@link FinalCancellationState} of the event after going through all listeners mutations to the state. Calling
     * {@link CancellationState#isCancelled()} will throw a {@link UnsupportedOperationException} if this event is not a {@link CancellableEvent}.
     * <p>
     * Calling {@link CancellationState#setCancelled(boolean)} on the returned {@link FinalCancellationState} will always throw
     * {@link UnsupportedOperationException}.
     * <p>
     * {@link CancellationState#isCancelled()} should only be called only once and on the thread that called
     * this method or else an {@link IllegalStateException} will be thrown.
     */
    @NotNull
    FinalCancellationState triggerEvent(final @NotNull T event);
}
