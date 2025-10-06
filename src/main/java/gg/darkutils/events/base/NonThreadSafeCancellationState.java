package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.BasicNonThreadSafeCancellationState;

/**
 * A non-thread safe implementation of {@link CancellationState}, which is fine for {@link EventHandler}
 * due to the contract of the method {@link EventHandler#triggerEvent(Event)} which runs all event listeners
 * sequentially on the caller thread.
 */
public non-sealed interface NonThreadSafeCancellationState extends CancellationState {
    /**
     * Returns a fresh {@link CancellationState}, defaulting to not cancelled.
     * <p>
     * The returned cancellation state is not thread-safe, but this is usually fine
     * as per the contract of {@link EventHandler#triggerEvent(Event)}, which calls
     * all listeners sequentially in the caller thread, so it won't be mutated by
     * multiple threads in parallel.
     *
     * @return A fresh {@link CancellationState}, defaulting to not cancelled.
     */
    static CancellationState ofFresh() {
        return new BasicNonThreadSafeCancellationState();
    }
}
