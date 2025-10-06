package gg.darkutils.events;

import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.NonCancellableEvent;
import gg.darkutils.feat.foraging.TreeMobSpawned;
import org.jetbrains.annotations.NotNull;

public record TreeGiftObtainedEvent(
        @NotNull TreeMobSpawned treeMobSpawned) implements NonCancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(TreeGiftObtainedEvent.class);
    }
}
