package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.NonThreadSafeCancellationState;

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
     * Creates a fresh {@link BasicNonThreadSafeCancellationState} defaulting to not cancelled.
     */
    public BasicNonThreadSafeCancellationState() {
        super();
    }

    @Override
    public final boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public final void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}
