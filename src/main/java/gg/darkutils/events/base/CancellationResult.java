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

    @NotNull
    public static final CancellationResult of(final boolean cancelled) {
        return cancelled ? CancellationResult.CANCELLED : CancellationResult.NOT_CANCELLED;
    }

    public final boolean isCancelled() {
        return CancellationResult.CANCELLED == this;
    }
}

