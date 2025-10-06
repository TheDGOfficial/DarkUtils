package gg.darkutils.events.base;

/**
 * Declares an {@link Event}, which is either {@link NonCancellableEvent} or {@link CancellableEvent}.
 */
public sealed interface Event permits NonCancellableEvent, CancellableEvent {
}
