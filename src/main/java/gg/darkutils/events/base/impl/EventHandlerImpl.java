package gg.darkutils.events.base.impl;

import gg.darkutils.DarkUtils;
import gg.darkutils.utils.JavaUtils;
import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationResult;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.EventListener;
import gg.darkutils.events.base.NonCancellableEvent;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * An {@link EventHandler} that implements all the specification of {@link EventHandler} interface.
 *
 * @param <T> The type of the event to handle.
 */
public final class EventHandlerImpl<T extends Event> implements EventHandler<T> {
    /**
     * Comparator for comparing {@link EventListener}s. Used to sort the listeners list.
     */
    @NotNull
    private static final Comparator<EventListener<? extends Event>> eventListenerPriorityComparator =
            Comparator.comparingInt((EventListener<? extends Event> eventListener) -> eventListener.priority().getValue()).reversed();
    /**
     * Holds all listeners of the event we are handling in a thread-safe manner (immutable list copy each time one added or removed).
     * Volatile attribute ensures the new list of listeners is directly visible to all threads.
     */
    @NotNull
    private volatile List<EventListener<? super Event>> listeners = List.of();

    /**
     * Creates a new {@link EventHandlerImpl}.
     */
    EventHandlerImpl(@NotNull final Class<T> eventClass) {
        super();

        if (CancellableEvent.class.isAssignableFrom(eventClass) && NonCancellableEvent.class.isAssignableFrom(eventClass)) {
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
        newList.sort((Comparator<? super EventListener<? super Event>>) EventHandlerImpl.eventListenerPriorityComparator);

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
            if (existing != (Object) listener) {
                newList.add(existing);
            }
        }

        // Already sorted by construction, no need to re-sort
        this.listeners = List.copyOf(newList);
    }

    @Override
    @NotNull
    public final <E extends Event & CancellableEvent> CancellationResult triggerCancellableEvent(@NotNull final E event) {
        final var cancellationState = event.cancellationState();

        final var localListeners = this.listeners;
        final var size = localListeners.size();

        switch (size) {
            case 0 -> {
                // No listeners - fast path
                return CancellationResult.NOT_CANCELLED;
            }
            case 1 -> {
                // Only one listener - fast path
                // We don't need to check isCancelled or receiveCancelled here as the event shouldn't be canceled yet.
                final var listener = localListeners.getFirst();
                try {
                    listener.accept(event);
                } catch (final Throwable error) {
                    EventHandlerImpl.handleListenerError(listener, event, error);
                }
                return CancellationResult.of(cancellationState.isCancelled());
            }
            case 2 -> {
                // Only two listeners - fast path
                // We don't need to check initial isCancelled or receiveCancelled for listener1 here as the event shouldn't be canceled yet.
                final var listener1 = localListeners.getFirst();
                final var listener2 = localListeners.getLast();
                try {
                    listener1.accept(event);
                } catch (final Throwable error) {
                    EventHandlerImpl.handleListenerError(listener1, event, error);
                }
                // Now check needed for isCancelled for listener2 in case listener1 cancelled it.
                var cancelled = cancellationState.isCancelled();
                if (!cancelled || listener2.receiveCancelled()) { // Need to check if listener2 wants to receiveCancelled
                    try {
                        listener2.accept(event);
                    } catch (final Throwable error) {
                        EventHandlerImpl.handleListenerError(listener2, event, error);
                    }
                    cancelled = cancellationState.isCancelled(); // Assign cancelled status in case listener2 cancelled or uncancelled it post listener1.
                }
                return CancellationResult.of(cancelled);
            }
        }

        // Fallback to slower path if 3 or more listeners
        var cancelled = false;

        // Using plain for loop instead of enhanced for loop prevents temporary iterator object allocation. Increases throughput if lots of events are triggered.
        for (var i = 0; size > i; ++i) {
            final var listener = localListeners.get(i);

            if (cancelled && !listener.receiveCancelled()) {
                continue;
            }

            try {
                listener.accept(event);
            } catch (final Throwable error) {
                EventHandlerImpl.handleListenerError(listener, event, error);
            }
            cancelled = cancellationState.isCancelled();
        }

        return CancellationResult.of(cancelled); // Returning CancellationResult instead of CancellationState ensures the caller can only inspect a finalized immutable canceled status, and can't mutate it. Furthermore, this avoids the state from escaping to the callers, potentially helping C2 optimize the mutable cancellation state object allocation. It seems to not fully optimize out the allocation at the moment, though, sadly, but in theory it should be able to.
    }

    @Override
    public final <E extends Event & NonCancellableEvent> void triggerNonCancellableEvent(@NotNull final E event) {
        final var localListeners = this.listeners;
        final var size = localListeners.size();

        switch (size) {
            case 0 -> {
                // No listeners - fast path
                return;
            }
            case 1 -> {
                // Only one listener - fast path
                final var listener = localListeners.getFirst();
                try {
                    listener.accept(event);
                } catch (final Throwable error) {
                    EventHandlerImpl.handleListenerError(listener, event, error);
                }
                return;
            }
            case 2 -> {
                // Only two listeners - fast path
                final var listener1 = localListeners.getFirst();
                final var listener2 = localListeners.getLast();
                try {
                    listener1.accept(event);
                } catch (final Throwable error) {
                    EventHandlerImpl.handleListenerError(listener1, event, error);
                }
                try {
                    listener2.accept(event);
                } catch (final Throwable error) {
                    EventHandlerImpl.handleListenerError(listener2, event, error);
                }
                return;
            }
        }

        // Fallback to slower path if 3 or more listeners

        // Using plain for loop instead of enhanced for loop prevents temporary iterator object allocation. Increases throughput if lots of events are triggered.
        for (var i = 0; size > i; ++i) {
            final var listener = localListeners.get(i);
            try {
                listener.accept(event);
            } catch (final Throwable error) {
                EventHandlerImpl.handleListenerError(listener, event, error);
            }
        }
    }

    private static final void handleListenerError(@NotNull final EventListener<? extends Event> listener, @NotNull final Event event, @NotNull final Throwable error) {
        if (DarkUtils.INSIDE_JUNIT) {
            throw JavaUtils.sneakyThrow(error);
        }

        final var actualListener = listener instanceof final EventListener.Impl<? extends Event> delegatingEventListener
                ? delegatingEventListener.listener()
                : listener;
        DarkUtils.error(EventHandlerImpl.class,
                "Error when executing listener " + actualListener.getClass().getName() +
                        " with priority " + listener.priority().name() +
                        " for event " + event.getClass().getName(), error);
    }

    @Override
    public final String toString() {
        return "EventHandlerImpl{" +
                "listeners=" + this.listeners +
                '}';
    }
}
