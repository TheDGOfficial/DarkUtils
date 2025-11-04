package gg.darkutils.events.base;

/**
 * Declares a {@link CancellationState} that always throws {@link UnsupportedOperationException} for all its operations.
 */
public interface NotCancellableCancellationState extends FinalCancellationState {
    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @return Always throws.
     */
    @Override
    default boolean isCancelled() {
        throw new UnsupportedOperationException("not cancellable");
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param cancelled is ignored as neither cancellation nor uncancellation is supported.
     */
    @Override
    default void setCancelled(final boolean cancelled) {
        throw new UnsupportedOperationException("not cancellable");
    }
}
