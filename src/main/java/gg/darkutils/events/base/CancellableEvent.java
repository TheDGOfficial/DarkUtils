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
}
