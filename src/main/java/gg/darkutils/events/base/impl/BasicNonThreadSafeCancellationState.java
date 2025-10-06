package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.*;

/**
 * A basic {@link CancellationState} with a simple mutable primitive boolean field.
 * <p>
 * Not thread-safe as it would require an {@link java.util.concurrent.atomic.AtomicBoolean} or synchronization,
 * but that's fine because {@link EventHandler} runs all listeners of an {@link Event} sequentially in the same thread.
 */
public final class BasicNonThreadSafeCancellationState implements NonThreadSafeCancellationState {
    /**
     * Keeps track of the cancellation state.
     */
    private boolean cancelled;

    /**
     * Creates a fresh  defaulting to not cancelled.
     */
    public BasicNonThreadSafeCancellationState() {
        super();
    }

    /**
     * Returns whether the state is cancelled or not.
     *
     * @return True if the state is cancelled, false otherwise.
     */
    @Override
    public final boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Sets the cancellation state.
     * <p>
     * See {@link EventListener} for the behaviour of uncancelling.
     *
     * @param cancelled True to cancel, false to uncancel.
     */
    @Override
    public final void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}
