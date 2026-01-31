package gg.darkutils.events.base.impl;

import gg.darkutils.DarkUtils;
import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.DelegatingEventListener;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.EventListener;
import gg.darkutils.events.base.EventPriority;
import gg.darkutils.events.base.CancellationResult;
import gg.darkutils.events.base.NonCancellableEvent;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * A basic {@link EventHandler} that implements all the specification of {@link EventHandler} interface.
 *
 * @param <T> The type of the event to handle.
 */
public final class BasicEventHandler<T extends Event> implements EventHandler<T> {
    /**
     * Comparator for comparing {@link EventListener}s. Used to sort the listeners list.
     */
    @NotNull
    private static final Comparator<EventListener<? extends Event>> eventListenerPriorityComparator =
            Comparator.comparingInt((EventListener<? extends Event> eventListener) -> eventListener.priority().getValue()).reversed();
    /**
     * Whether this event handler is for a {@link CancellableEvent} or not.
     */
    private final boolean cancellableEvent;
    /**
     * Holds all listeners of the event we are handling in a thread-safe manner (immutable list copy each time one added or removed).
     * Volatile attribute ensures the new list of listeners is directly visible to all threads.
     */
    @NotNull
    private volatile List<EventListener<? super Event>> listeners = List.of();

    /**
     * Creates a new {@link BasicEventHandler}.
     */
    BasicEventHandler(@NotNull final Class<T> eventClass) {
        super();

        this.cancellableEvent = CancellableEvent.class.isAssignableFrom(eventClass);

        if (this.cancellableEvent && NonCancellableEvent.class.isAssignableFrom(eventClass)) {
            // Sanity check
            throw new IllegalArgumentException("Event class " + eventClass.getName() + " implements both CancellableEvent and NonCancellableEvent");
        }
    }

    @Override
    @NotNull
    public final Iterable<EventListener<? super Event>> getListeners() {
        return this.listeners;
    }

    @Override
    public final void addListener(@NotNull final EventListener<? super Event> listener) {
        final var current = this.listeners;

        if (current.contains(listener)) {
            throw new IllegalStateException("listener is already registered");
        }

        final var newList = new ReferenceArrayList<>(current);
        newList.add(listener);
        newList.sort(BasicEventHandler.eventListenerPriorityComparator);

        // Single volatile write = safe publication
        this.listeners = List.copyOf(newList);
    }

    @Override
    public final void removeListener(@NotNull final EventListener<T> listener) {
        final var current = this.listeners;

        if (!current.contains(listener)) {
            throw new IllegalStateException("tried to remove a listener that is not registered");
        }

        final var newList = new ReferenceArrayList<EventListener<? super Event>>(current.size() - 1);
        for (final var existing : current) {
            if (existing != listener) {
                newList.add(existing);
            }
        }

        // Already sorted by construction, no need to re-sort
        this.listeners = List.copyOf(newList);
    }

    @Override
    @NotNull
    public final <E extends Event & CancellableEvent> CancellationResult triggerCancellableEvent(@NotNull final E event) {
        // Sanity-check
        assert this.cancellableEvent : "triggerEvent(CancellableEvent) called on a BasicEventHandler<NonCancellableEvent>";

        final var cancellationState = event.cancellationState();

        final var localListeners = this.listeners;
        final var size = localListeners.size();

        if (0 == size) {
            // No listeners - fast path
            return CancellationResult.NOT_CANCELLED;
        }

        if (1 == size) {
            // Only one listener - fast path
            // We don't need to check isCancelled or receiveCancelled here as the even't shouldn't be cancelled yet.
            // We have an assertion just in case though, if -ea is enabled as a JVM argument.
            assert !cancellationState.isCancelled() : "fresh CancellationState was in cancelled state before any listener invocation";
            final var listener = localListeners.getFirst();
            try {
                listener.onEvent(event);
            } catch (final Throwable error) {
                BasicEventHandler.handleListenerError(listener, event, error);
            }
            return CancellationResult.of(cancellationState.isCancelled());
        }

        // Fallback to slower path if 2 or more listeners
        var cancelled = false;

        // Using plain for loop instead of enhanced for loop prevents temporary iterator object allocation. Increases throughput if lots of events are triggered.
        for (int i = 0; size > i; ++i) {
            final var listener = localListeners.get(i);

            if (cancelled && !listener.receiveCancelled()) {
                continue;
            }

            try {
                listener.onEvent(event);
            } catch (final Throwable error) {
                BasicEventHandler.handleListenerError(listener, event, error);
            }

            cancelled = cancellationState.isCancelled();
        }

        return CancellationResult.of(cancelled); // Returning CancellationResult instead of CancellationState ensures the caller can only inspect a finalized immutable cancelled status, and can't mutate it. Furthermore, this avoids the state from escaping to the callers, potentially helping C2 optimize the mutable cancellation state object allocation. It seems to not fully optimize out the allocation at the moment, though, sadly, but in theory it should be able to.
    }

    @Override
    public final <E extends Event & NonCancellableEvent> void triggerNonCancellableEvent(@NotNull final E event) {
        // Sanity-check
        assert !this.cancellableEvent : "triggerEvent(NonCancellableEvent) called on a BasicEventHandler<CancellableEvent>";

        final var localListeners = this.listeners;
        final int size = localListeners.size();

        if (0 == size) {
            // No listeners - fast path
            return;
        }

        if (1 == size) {
            // Only one listener - fast path
            final var listener = localListeners.getFirst();
            try {
                listener.onEvent(event);
            } catch (final Throwable error) {
                BasicEventHandler.handleListenerError(listener, event, error);
            }
            return;
        }

        // Using plain for loop instead of enhanced for loop prevents temporary iterator object allocation. Increases throughput if lots of events are triggered.
        for (int i = 0; size > i; ++i) {
            final var listener = localListeners.get(i);
            try {
                listener.onEvent(event);
            } catch (final Throwable error) {
                BasicEventHandler.handleListenerError(listener, event, error);
            }
        }
    }

    private static final void handleListenerError(@NotNull final EventListener<? extends Event> listener, @NotNull final Event event, @NotNull final Throwable error) {
        final var actualListener = listener instanceof final DelegatingEventListener<? extends Event> delegatingEventListener
                ? delegatingEventListener.listener()
                : listener;
        DarkUtils.error(BasicEventHandler.class,
                "Error when executing listener " + actualListener.getClass().getName() +
                        " with priority " + actualListener.priority().name() +
                        " for event " + event.getClass().getName(), error);
    }

    @Override
    public final String toString() {
        return "BasicEventHandler{" +
                "cancellableEvent=" + this.cancellableEvent +
                ", listeners=" + this.listeners +
                '}';
    }
}
