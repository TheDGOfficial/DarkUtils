package gg.darkutils.events;

import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.NonCancellableEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers when the mod's config finishes saving to the disk.
 */
public record ConfigSaveFinishEvent() implements NonCancellableEvent {
    /**
     * Singleton instance since this is a stateless event.
     */
    @NotNull
    public static final ConfigSaveFinishEvent INSTANCE = new ConfigSaveFinishEvent();
}
