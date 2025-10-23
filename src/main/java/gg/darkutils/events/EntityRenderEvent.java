package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers before an entity has been rendered.
 * <p>
 * Cancelling will make the entity not render.
 *
 * @param cancellationState The cancellation state holder.
 * @param entity            The entity.
 */
public record EntityRenderEvent(@NotNull CancellationState cancellationState,
                                @NotNull Entity entity) implements CancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(EntityRenderEvent.class);
    }

    /**
     * Creates a new {@link EntityRenderEvent} suitable for triggering the event.
     * A cached {@link CancellationState#ofCached()} will be used with non-cancelled state by default.
     *
     * @param entity The entity.
     */
    public EntityRenderEvent(@NotNull final Entity entity) {
        this(CancellationState.ofCached(), entity);
    }
}
