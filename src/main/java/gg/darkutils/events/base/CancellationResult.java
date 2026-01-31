package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a finalized version of {@link CancellationState}.
 */
public enum CancellationResult {
    CANCELLED,
    NOT_CANCELLED;

    private CancellationResult() {
    }

    public final boolean isCancelled() {
        return this == CancellationResult.CANCELLED;
    }

    @NotNull
    public static final CancellationResult of(final boolean cancelled) {
        return cancelled ? CancellationResult.CANCELLED : CancellationResult.NOT_CANCELLED;
    }
}

