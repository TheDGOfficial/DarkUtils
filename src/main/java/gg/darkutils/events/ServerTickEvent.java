package gg.darkutils.events;

import gg.darkutils.events.base.NonCancellableEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers when the server finishes a tick.
 */
public record ServerTickEvent() implements NonCancellableEvent {
    /**
     * Singleton instance since this is a stateless event.
     */
    @NotNull
    public static final ServerTickEvent INSTANCE = new ServerTickEvent();
}
