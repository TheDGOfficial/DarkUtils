package gg.darkutils.events.base;

/**
 * A final {@link CancellationState} that has the cancellation result that can't be changed and only queried once on the
 * correct thread. The implementation should verify all these constraints.
 */
public non-sealed interface FinalCancellationState extends CancellationState {
    @Override
    public default void setCancelled(final boolean cancelled) {
        throw new UnsupportedOperationException(
                "Calling setCancelled() on a final cancellation state is not supported"
        );
    }
}
