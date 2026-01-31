package gg.darkutils.events.base;

/**
 * Declares a {@link CancellationState}.
 */
public sealed interface CancellationState permits NonThreadSafeCancellationState {
    /**
     * Returns a fresh {@link CancellationState}, defaulting to not canceled.
     *
     * @return A fresh {@link CancellationState}, defaulting to not canceled.
     */
    static CancellationState ofFresh() {
        return NonThreadSafeCancellationState.ofFresh();
    }

    /**
     * Checks whether this state is canceled or not.
     *
     * @return Whether this state is canceled or not.
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of this {@link CancellationState}.
     *
     * @param cancelled Whether this state should report as canceled or not.
     */
    void setCancelled(final boolean cancelled);

    /**
     * Marks this {@link CancellationState} as canceled.
     */
    default void cancel() {
        this.setCancelled(true);
    }

    /**
     * Uncancels this {@link CancellationState} by marking it as not canceled.
     */
    default void uncancel() {
        this.setCancelled(false);
    }
}
