package gg.darkutils.events.base.impl;

import gg.darkutils.DarkUtils;
import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.DelegatingEventListener;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.EventListener;
import gg.darkutils.events.base.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
     */
    @NotNull
    private final AtomicReference<List<EventListener<T>>> listeners = new AtomicReference<>(List.of());

    /**
     * Creates a new {@link BasicEventHandler}.
     */
    BasicEventHandler(@NotNull final Class<T> eventClass) {
        super();

        this.cancellableEvent = CancellableEvent.class.isAssignableFrom(eventClass);
    }

    /**
     * Sorts listeners based on their {@link EventPriority}.
     */
    private final void sortListeners() {
        this.listeners.updateAndGet(listeners -> listeners.parallelStream()
                .sorted(BasicEventHandler.eventListenerPriorityComparator)
                .toList());
    }

    @Override
    @NotNull
    public final Iterable<EventListener<T>> getListeners() {
        return this.listeners.get();
    }

    @Override
    public final void addListener(@NotNull final EventListener<T> listener) {
        if (this.listeners.get().contains(listener)) {
            throw new IllegalStateException("listener is already registered");
        }
        this.listeners.updateAndGet(listeners -> {
            final var newList = new ArrayList<>(listeners);
            newList.add(listener);
            return List.copyOf(newList);
        });
        this.sortListeners();
    }

    @Override
    public final void removeListener(@NotNull final EventListener<T> listener) {
        if (!this.listeners.get().contains(listener)) {
            throw new IllegalStateException("tried to remove a listener that is not registered");
        }
        this.listeners.updateAndGet(listeners -> listeners.parallelStream()
                .filter(eventListener -> eventListener != listener)
                .toList());
        this.sortListeners();
    }

    @Override
    @NotNull
    public final CancellationState triggerEvent(@NotNull final T event) {
        if (this.cancellableEvent) {
            return this.triggerCancellableEvent(event, ((CancellableEvent) event).cancellationState());
        }

        this.triggerNonCancellableEvent(event);
        return CancellationState.ofNotCancellable();
    }

    @NotNull
    private final CancellationState triggerCancellableEvent(@NotNull final T event, @NotNull final CancellationState cancellationState) {
        // Using plain for loop instead of enhanced for loop prevents temporary iterator object allocation. Increases throughput if lots of events are triggered.
        final var localListeners = this.listeners.get();
        for (int i = 0, len = localListeners.size(); len > i; ++i) {
            final var listener = localListeners.get(i);

            final var cancelled = cancellationState.isCancelled();

            if (cancelled && !listener.receiveCancelled()) {
                continue;
            }

            try {
                listener.onEvent(event);
            } catch (final Throwable error) {
                this.handleListenerError(listener, event, error);
            }
        }

        return cancellationState;
    }

    private final void triggerNonCancellableEvent(@NotNull final T event) {
        // Using plain for loop instead of enhanced for loop prevents temporary iterator object allocation. Increases throughput if lots of events are triggered.
        final var localListeners = this.listeners.get();
        for (int i = 0, len = localListeners.size(); len > i; ++i) {
            final var listener = localListeners.get(i);
            try {
                listener.onEvent(event);
            } catch (final Throwable error) {
                this.handleListenerError(listener, event, error);
            }
        }
    }

    private final void handleListenerError(@NotNull final EventListener<T> listener, @NotNull final T event, @NotNull final Throwable error) {
        final var actualListener = listener instanceof final DelegatingEventListener<T> delegatingEventListener
                ? delegatingEventListener.listener()
                : listener;
        DarkUtils.error(BasicEventHandler.class,
                "Error when executing listener " + actualListener.getClass().getName() +
                        " with priority " + actualListener.priority().name() +
                        " for event " + event.getClass().getName(), error);
    }
}
