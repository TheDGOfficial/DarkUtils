package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.FinalCancellationState;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

/**
 * A basic {@link FinalCancellationState} with a simple immutable primitive final boolean field.
 * <p>
 * Safety-checks to ensure isCancelled is called only once and on the correct thread are in place.
 */
public final class BasicFinalCancellationState implements FinalCancellationState {
    private final @NotNull WeakReference<Thread> owner = new WeakReference<>(Thread.currentThread());
    private final boolean cancelled;

    private BasicFinalCancellationState(final boolean cancelled) {
        super();

        this.cancelled = cancelled;
    }

    @NotNull
    public static final BasicFinalCancellationState of(final boolean cancelled) {
        return new BasicFinalCancellationState(cancelled);
    }

    private final void ensureOwnerThreadAccess() {
        final var currentThread = Thread.currentThread();
        final var ownerThread = this.owner.get();

        if (null == ownerThread) {
            throw new IllegalStateException(
                    "FinalCancellationState has already been queried or escaped its owner thread's lifetime! " +
                            "isCancelled() can only be called once. Access attempted from thread: " + currentThread.getName()
            );
        }

        if (currentThread != ownerThread) {
            throw new IllegalStateException(
                    "FinalCancellationState accessed from the wrong thread " + currentThread.getName() +
                            ". Thread " + ownerThread.getName() + " owns this state."
            );
        }
    }

    @Override
    public final boolean isCancelled() {
        this.ensureOwnerThreadAccess();
        this.owner.clear(); // only allow one call

        return this.cancelled;
    }

    @Override
    public final void setCancelled(final boolean cancelled) {
        throw new UnsupportedOperationException(
                "Calling setCancelled() on " + FinalCancellationState.class.getSimpleName() + " is not supported"
        );
    }
}

