package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers before an entity will be interacted by the player.
 * <p>
 * Cancelling will make the interaction not happen.
 *
 * @param cancellationState The cancellation state holder.
 * @param entity            The entity.
 */
public record InteractEntityEvent(@NotNull CancellationState cancellationState,
                                  @NotNull Entity entity) implements CancellableEvent {
    /**
     * Creates a new {@link InteractEntityEvent} suitable for triggering the event.
     * A cached {@link CancellationState#ofCached()} will be used with non-canceled state by default.
     *
     * @param entity The entity.
     */
    public InteractEntityEvent(@NotNull final Entity entity) {
        this(CancellationState.ofFresh(), entity);
    }
}
