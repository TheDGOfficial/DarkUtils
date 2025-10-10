package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.BasicNotCancellableCancellationState;

/**
 * Declares a {@link CancellationState}.
 */
public sealed interface CancellationState permits NonThreadSafeCancellationState, NotCancellableCancellationState {
    /**
     * Returns a cached {@link CancellationState}, defaulting to not cancelled.
     * <p>
     * The returned cancellation state is cached per thread with {@link ThreadLocal} to reduce allocations.
     * <p>
     * This makes the shared {@link CancellationState}s itself thread-safe, while keeping the underlying boolean
     * not thread-safe in the sense that writes might not be able to be seen by other threads or concurrent writes
     * overriding each other with random order.
     * <p>
     * However, this is usually fine as per the contract of {@link EventHandler#triggerEvent(Event)}, which calls
     * all listeners sequentially in the caller thread, so it won't be mutated by multiple threads in parallel.
     *
     * @return A cached {@link CancellationState}, defaulting to not cancelled.
     */
    static CancellationState ofCached() {
        return NonThreadSafeCancellationState.ofCached();
    }

    /**
     * Returns a {@link CancellationState} that always throws {@link UnsupportedOperationException}.
     *
     * @return A {@link CancellationState} that always throws {@link UnsupportedOperationException}.
     */
    static CancellationState ofNotCancellable() {
        return BasicNotCancellableCancellationState.getInstance();
    }

    /**
     * Checks whether this state is cancelled or not.
     *
     * @return Whether this state is cancelled or not.
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of this {@link CancellationState}.
     *
     * @param cancelled Whether this state should report as cancelled or not.
     */
    void setCancelled(final boolean cancelled);

    /**
     * Marks this {@link CancellationState} as cancelled.
     */
    default void cancel() {
        this.setCancelled(true);
    }

    /**
     * Uncancels this {@link CancellationState} by marking it as not cancelled.
     */
    default void uncancel() {
        this.setCancelled(false);
    }
}
