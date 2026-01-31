package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.BasicNonThreadSafeCancellationState;

/**
 * A non-thread safe implementation of {@link CancellationState}, which is fine for {@link EventHandler}
 * due to the contract of the method {@link EventHandler#triggerEvent(Event)} which runs all event listeners
 * sequentially on the caller thread.
 */
public non-sealed interface NonThreadSafeCancellationState extends CancellationState {
    /**
     * Returns a fresh {@link NonThreadSafeCancellationState}, defaulting to not canceled.
     *
     * @return A fresh {@link NonThreadSafeCancellationState}, defaulting to not canceled.
     */
    static CancellationState ofFresh() {
        return BasicNonThreadSafeCancellationState.ofFresh();
    }
}
