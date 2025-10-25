package gg.darkutils.events;

import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.NonCancellableEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers when the world is rendering.
 */
public record RenderWorldEvent() implements NonCancellableEvent {
    /**
     * Singleton instance since this is a stateless event.
     */
    @NotNull
    public static final RenderWorldEvent INSTANCE = new RenderWorldEvent();

    static {
        EventRegistry.centralRegistry().registerEvent(RenderWorldEvent.class);
    }
}
