package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.NonCancellableEvent;
import gg.darkutils.events.base.NotCancellableCancellationState;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@link CancellationState} for a {@link NonCancellableEvent}.
 */
public final class BasicNotCancellableCancellationState implements NotCancellableCancellationState {
    /**
     * Singleton instance to reduce allocations.
     */
    @NotNull
    private static final BasicNotCancellableCancellationState INSTANCE = new BasicNotCancellableCancellationState();

    private BasicNotCancellableCancellationState() {
        super();
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    @NotNull
    public static final BasicNotCancellableCancellationState getInstance() {
        return BasicNotCancellableCancellationState.INSTANCE;
    }
}
