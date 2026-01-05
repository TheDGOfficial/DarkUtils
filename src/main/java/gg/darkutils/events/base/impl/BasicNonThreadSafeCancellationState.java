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
public final class BasicNonThreadSafeCancellationState implements NonThreadSafeCancellationState, AutoCloseable {
    /**
     * Holds the {@link ThreadLocal} of the shared {@link BasicNonThreadSafeCancellationState} instance.
     */
    @NotNull
    private static final ThreadLocal<BasicNonThreadSafeCancellationState> INSTANCE = ThreadLocal.withInitial(BasicNonThreadSafeCancellationState::new);
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
     * Keeps track of the cancellation state.
     */
    private boolean cancelled;

    /**
     * Creates a new {@link BasicNonThreadSafeCancellationState} defaulting to not canceled.
     */
    private BasicNonThreadSafeCancellationState() {
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

    private final void ensureOwnerThreadAccess() {
        final var ownerThreadId = this.ownerId;

        if (-1L == ownerThreadId) {
            // The state has been closed (owner cleared) or the owner thread finished its work and got GCed
            throw new IllegalStateException(
                    "Cancellation state escaped its owner thread's lifetime! " +
                            "It cannot be accessed from thread: " + Thread.currentThread().getName()
            );
        }

        final var currentThread = Thread.currentThread();
        final var currentThreadId = currentThread.threadId();

        if (currentThreadId != ownerThreadId) {
            // Access from a different live thread
            throw new IllegalStateException(
                    "Cancellation state accessed from the wrong thread " + currentThread.getName() +
                            ". Thread " + this.ownerName + " owns this state and it must only be accessed from that thread."
            );
        }
    }

    @Override
    public final boolean isCancelled() {
        this.ensureOwnerThreadAccess();

        return this.cancelled;
    }

    @Override
    public final void setCancelled(final boolean cancelled) {
        this.ensureOwnerThreadAccess();

        this.cancelled = cancelled;
    }

    @Override
    public final void reset() {
        final var currentThread = Thread.currentThread();

        this.ownerId = currentThread.threadId();
        this.ownerName = currentThread.getName();

        NonThreadSafeCancellationState.super.reset();
    }

    @Override
    public final void close() {
        if (-1L == this.ownerId) {
            throw new IllegalStateException("called close() before reset() - possible double close()");
        }

        // .isCancelled() or .setCancelled() after this point will throw an error.
        this.ownerId = -1L;
    }

    @Override
    public final String toString() {
        return "BasicNonThreadSafeCancellationState{" +
                "ownerId=" + this.ownerId +
                ", ownerName='" + this.ownerName + '\'' +
                ", cancelled=" + this.cancelled +
                '}';
    }
}
