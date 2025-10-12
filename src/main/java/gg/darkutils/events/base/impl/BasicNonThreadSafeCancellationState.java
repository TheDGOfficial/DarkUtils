package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.NonThreadSafeCancellationState;
import org.jetbrains.annotations.NotNull;

/**
 * A basic {@link CancellationState} with a simple mutable primitive boolean field.
 * <p>
 * If using {@link BasicNonThreadSafeCancellationState#getCachedInstanceForCurrentThread()} for low allocation pressure,
 * the returned cancellation state is cached per thread with {@link ThreadLocal} to reduce allocations.
 * <p>
 * This makes the shared {@link CancellationState}s itself thread-safe, while keeping the underlying boolean
 * not thread-safe in the sense that writes might not be able to be seen by other threads or concurrent writes
 * overriding each other with random order.
 * <p>
 * However, this is usually fine as per the contract of {@link EventHandler#triggerEvent(Event)}, which calls
 * all listeners sequentially in the caller thread, so it won't be mutated by multiple threads in parallel.
 */
public final class BasicNonThreadSafeCancellationState implements NonThreadSafeCancellationState {
    /**
     * Holds the {@link ThreadLocal} of the shared {@link BasicNonThreadSafeCancellationState} instance.
     */
    @NotNull
    private static final ThreadLocal<BasicNonThreadSafeCancellationState> INSTANCE = ThreadLocal.withInitial(BasicNonThreadSafeCancellationState::new);
    /**
     * Keeps track of the cancellation state.
     */
    private boolean cancelled;

    /**
     * Creates a new {@link BasicNonThreadSafeCancellationState} defaulting to not cancelled.
     */
    public BasicNonThreadSafeCancellationState() {
        super();
    }

    /**
     * Gets the shared {@link BasicNonThreadSafeCancellationState} wrapped in a {@link ThreadLocal} for the current thread.
     *
     * @return The shared {@link BasicNonThreadSafeCancellationState} wrapped in a {@link ThreadLocal} for the current thread.
     */
    @NotNull
    public static final BasicNonThreadSafeCancellationState getCachedInstanceForCurrentThread() {
        final var cached = BasicNonThreadSafeCancellationState.INSTANCE.get();
        cached.reset();

        return cached;
    }

    @Override
    public final boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public final void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}
