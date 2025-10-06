package gg.darkutils.events.base;

/**
 * Marks a non-cancellable {@link Event}.
 * <p>
 * These should not have {@link CancellationState}.
 */
public non-sealed interface NonCancellableEvent extends Event {
}
