package gg.darkutils.events;

import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.NonCancellableEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers before the mod's config screen is going to be opened.
 */
public record ConfigScreenOpenEvent() implements NonCancellableEvent {
    /**
     * Singleton instance since this is a stateless event.
     */
    @NotNull
    public static final ConfigScreenOpenEvent INSTANCE = new ConfigScreenOpenEvent();

    static {
        EventRegistry.centralRegistry().registerEvent(ConfigScreenOpenEvent.class);
    }
}
