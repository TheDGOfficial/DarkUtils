package gg.darkutils.events;

import gg.darkutils.events.base.NonCancellableEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers when the mod's config is going to be saved to the disk.
 */
public record ConfigSaveStartEvent() implements NonCancellableEvent {
    /**
     * Singleton instance since this is a stateless event.
     */
    @NotNull
    public static final ConfigSaveStartEvent INSTANCE = new ConfigSaveStartEvent();
}
