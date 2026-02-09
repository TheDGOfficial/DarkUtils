package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a {@link CancellableEvent}, which will have a {@link CancellationState}.
 */
@FunctionalInterface
public non-sealed interface CancellableEvent extends Event {
    /**
     * Returns the {@link CancellationState} which can be used to cancel the {@link Event}.
     *
     * @return The {@link CancellationState} which can be used to cancel the {@link Event}.
     */
    @NotNull
    CancellationState cancellationState();

    /**
     * Triggers this cancellable event in the central event registry, and returns the final cancellation state.
     * <p>
     * This is exactly equivalent and is a shortcut method for the following call:
     * {@snippet :
     * EventRegistry.centralRegistry().triggerEvent(this);
     *}
     * <p>
     * The returned value should further be checked to see if it is canceled or not.
     * Alternatively, use {@link CancellableEvent#triggerAndCancelled()}.
     */
    @NotNull
    default CancellationResult trigger() {
        return EventRegistry.centralRegistry().triggerEvent(this);
    }

    /**
     * Triggers this cancellable event in the central event registry, and checks if it is canceled.
     * <p>
     * All the below perform the same check, just some are shorter:
     * {@snippet :
     * // shortest
     * if (this.triggerAndCancelled()) {
     *  // event was cancelled
     * }
     *
     * // still short
     * if (this.trigger().isCancelled()) {
     *  // event was cancelled
     * }
     *
     * // longest
     * if (EventRegistry.centralRegistry().triggerEvent(this).isCancelled()) {
     *  // event was cancelled
     * }
     *}
     * <p>
     * It is a preference which one you use, but this is recommended over plain trigger as it encapsulates
     * the is canceled call inside this method, preventing you from accidentally calling it 2 times which throws.
     */
    default boolean triggerAndCancelled() {
        return EventRegistry.centralRegistry().triggerEvent(this).isCancelled();
    }

    /**
     * Triggers this cancellable event in the central event registry, and checks if it's NOT canceled.
     * <p>
     * All the below perform the same check, just some are shorter:
     * {@snippet :
     * // short
     * if (this.triggerAndNotCancelled()) {
     *  // event was NOT cancelled
     * }
     *
     * // alternative, shorter but you have to negate manually with !
     * if (!this.triggerAndCancelled()) { // note the negation with !
     *  // event was NOT cancelled
     * }
     *
     * // still short
     * if (!this.trigger().isCancelled()) {
     *  // event was NOT cancelled
     * }
     *
     * // longest
     * if (!EventRegistry.centralRegistry().triggerEvent(this).isCancelled()) {
     *  // event was NOT cancelled
     * }
     *}
     * <p>
     * It is a preference which one you use, but this is recommended over manually negating, preventing
     * you from possibly forgetting to negate the canceled boolean.
     */
    default boolean triggerAndNotCancelled() {
        return !EventRegistry.centralRegistry().triggerEvent(this).isCancelled();
    }
}
