package gg.darkutils.events;

import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.NonCancellableEvent;
import gg.darkutils.feat.foraging.TreeMobSpawned;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers when a Tree Gift has been obtained.
 *
 * @param treeMobSpawned The mob spawned as the result of this Tree Gift,
 *                       can be {@link TreeMobSpawned#NONE} to indicate none spawned.
 */
public record TreeGiftObtainedEvent(
        @NotNull TreeMobSpawned treeMobSpawned) implements NonCancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(TreeGiftObtainedEvent.class);
    }
}
