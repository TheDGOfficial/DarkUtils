package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.FinalCancellationState;
import org.jetbrains.annotations.NotNull;

/**
 * A basic {@link FinalCancellationState} with a simple immutable primitive final boolean field.
 * <p>
 * Safety-checks to ensure isCancelled is called only once and on the correct thread are in place.
 */
public final class BasicFinalCancellationState implements FinalCancellationState {
    /**
     * Holds the {@link ThreadLocal} of the shared {@link BasicFinalCancellationState} instance for not canceled status.
     */
    @NotNull
    private static final ThreadLocal<BasicFinalCancellationState> NOT_CANCELLED = ThreadLocal.withInitial(() -> new BasicFinalCancellationState(false));

    /**
     * Holds the {@link ThreadLocal} of the shared {@link BasicFinalCancellationState} instance for canceled status.
     */
    @NotNull
    private static final ThreadLocal<BasicFinalCancellationState> CANCELLED = ThreadLocal.withInitial(() -> new BasicFinalCancellationState(true));
    /**
     * The cancellation state.
     */
    private final boolean cancelled;
    /**
     * Holds the owner thread id of this instance.
     */
    private long ownerId = Thread.currentThread().threadId();
    /**
     * Holds the owner thread name of this instance.
     * <p>
     * Used only for aiding in debugging for example in error messages.
     */
    @NotNull
    private String ownerName = Thread.currentThread().getName();

    /**
     * Creates a new {@link BasicFinalCancellationState} with the given canceled state.
     */
    private BasicFinalCancellationState(final boolean cancelled) {
        super();

        this.cancelled = cancelled;
    }

    /**
     * Returns a cached instance for current thread for the given canceled state.
     *
     * @return A cached instance for current thread for the given canceled state.
     */
    @NotNull
    public static final BasicFinalCancellationState ofCached(final boolean cancelled) {
        final var state = (cancelled ? BasicFinalCancellationState.CANCELLED : BasicFinalCancellationState.NOT_CANCELLED).get();
        state.reset();

        return state;
    }

    private final void ensureOwnerThreadAccess() {
        final var ownerThreadId = this.ownerId;

        if (-1L == ownerThreadId) {
            throw new IllegalStateException(
                    "Final cancellation state has already been queried or escaped its owner thread's lifetime! " +
                            "isCancelled() can only be called once. Access attempted from thread: " + Thread.currentThread().getName()
            );
        }

        final var currentThread = Thread.currentThread();
        final var currentThreadId = currentThread.threadId();

        if (currentThreadId != ownerThreadId) {
            throw new IllegalStateException(
                    "Final cancellation state accessed from the wrong thread " + currentThread.getName() +
                            ". Thread " + this.ownerName + " owns this state."
            );
        }
    }

    @Override
    public final void reset() {
        final var currentThread = Thread.currentThread();

        this.ownerId = currentThread.threadId();
        this.ownerName = currentThread.getName();
    }

    @Override
    public final boolean isCancelled() {
        this.ensureOwnerThreadAccess();
        this.ownerId = -1L; // only allow one call

        return this.cancelled;
    }

    @Override
    public final String toString() {
        return "BasicFinalCancellationState{" +
                "cancelled=" + this.cancelled +
                ", ownerId=" + this.ownerId +
                ", ownerName='" + this.ownerName + '\'' +
                '}';
    }
}

