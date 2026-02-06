package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.DelegatingEventListener;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventListener;
import gg.darkutils.events.base.EventPriority;
import org.jetbrains.annotations.NotNull;

/**
 * An event listener implementation that delegates to another with custom priority and receiveCancelled behavior.
 *
 * @param listener         The base listener to be used as a delegate.
 * @param priority         The custom {@link EventPriority} priority.
 * @param receiveCancelled The custom receiveCancelled behavior.
 * @param <T>              The type of the event.
 */
public record EventListenerImpl<T extends Event>(@NotNull EventListener<T> listener, @NotNull EventPriority priority,
                                                  boolean receiveCancelled) implements DelegatingEventListener<T> {
}
