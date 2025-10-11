package gg.darkutils.events.base.impl;

import gg.darkutils.DarkUtils;
import gg.darkutils.events.base.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

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
     * Holds all listeners of the event we are handling in a thread-safe manner.
     */
    @NotNull
    private final CopyOnWriteArrayList<EventListener<T>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Whether this event handler is for a {@link CancellableEvent} or not.
     */
    private final boolean cancellableEvent;

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
        this.listeners.sort(BasicEventHandler.eventListenerPriorityComparator);
    }

    @Override
    @NotNull
    public final Iterable<EventListener<T>> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    @Override
    public final void addListener(@NotNull final EventListener<T> listener) {
        this.listeners.add(listener);
        this.sortListeners();
    }

    @Override
    public final void removeListener(@NotNull final EventListener<T> listener) {
        this.listeners.remove(listener);
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
        for (final var listener : this.listeners) {
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
        for (final var listener : this.listeners) {
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
