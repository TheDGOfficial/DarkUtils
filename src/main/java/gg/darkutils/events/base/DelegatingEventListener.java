package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

/**
 * An {@link EventListener} that delegates to another {@link EventListener}
 * for any purposes possible.
 *
 * @param <T> The type of the event.
 */
public interface DelegatingEventListener<T extends Event> extends EventListener<T> {
    /**
     * Returns the actual, underlying {@link EventListener}.
     *
     * @return The actual, underlying {@link EventListener}.
     */
    @NotNull
    EventListener<T> listener();
}
