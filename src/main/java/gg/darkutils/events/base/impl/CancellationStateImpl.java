package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link CancellationState} with a simple mutable primitive boolean field.
 * <p>
 * This class does not override {@link Object#equals(Object)} or {@link Object#hashCode()}.
 * Therefore, those operations are unsupported and will use reference hash code and equality.
 */
public final class CancellationStateImpl implements CancellationState {
    /**
     * Keeps track of the cancellation state.
     */
    private boolean cancelled;

    /**
     * Creates a new {@link CancellationStateImpl} defaulting to not canceled.
     */
    private CancellationStateImpl() {
        super();
    }

    /**
     * Returns a fresh {@link CancellationStateImpl}, defaulting to not canceled.
     *
     * @return A fresh {@link CancellationStateImpl}, defaulting to not canceled.
     */
    @NotNull
    public static final CancellationStateImpl ofFresh() {
        return new CancellationStateImpl();
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
        return "CancellationStateImpl{cancelled=" + this.cancelled + '}';
    }
}
