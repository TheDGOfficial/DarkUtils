package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.NonThreadSafeCancellationState;
import org.jetbrains.annotations.NotNull;

/**
 * A basic {@link CancellationState} with a simple mutable primitive boolean field.
 * <p>
 * This class does not override {@link Object#equals(Object)} or {@link Object#hashCode()}.
 * Therefore, those operations are unsupported and will use reference hash code and equality.
 */
public final class BasicNonThreadSafeCancellationState implements NonThreadSafeCancellationState {
    /**
     * Keeps track of the cancellation state.
     */
    private boolean cancelled;

    /**
     * Creates a new {@link BasicNonThreadSafeCancellationState} defaulting to not canceled.
     */
    private BasicNonThreadSafeCancellationState() {
        super();
    }

    /**
     * Returns a fresh {@link BasicNonThreadSafeCancellationState}, defaulting to not canceled.
     *
     * @return A fresh {@link BasicNonThreadSafeCancellationState}, defaulting to not canceled.
     */
    @NotNull
    public static final BasicNonThreadSafeCancellationState ofFresh() {
        return new BasicNonThreadSafeCancellationState();
    }

    @Override
    public final boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public final void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public final String toString() {
        return "BasicNonThreadSafeCancellationState{cancelled=" + this.cancelled + '}';
    }
}
