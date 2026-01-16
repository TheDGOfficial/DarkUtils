package gg.darkutils.events.base;

/**
 * Marks a non-cancellable {@link Event}.
 * <p>
 * These should not have {@link CancellationState}.
 */
public non-sealed interface NonCancellableEvent extends Event {
    /**
     * Triggers this non-cancellable event in the central event registry.
     * <p>
     * This is exactly equivalent and is a shortcut method for the following call:
     * {@snippet :
     * EventRegistry.centralRegistry().triggerEvent(this)
     *}
     */
    default void trigger() {
        EventRegistry.centralRegistry().triggerEvent(this);
    }
}
