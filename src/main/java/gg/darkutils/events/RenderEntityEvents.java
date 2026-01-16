package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;

public final class RenderEntityEvents {
    private RenderEntityEvents() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    /**
     * Triggers before an armor stand entity has been rendered.
     * <p>
     * Cancelling will make the armor stand entity not render.
     *
     * @param cancellationState The cancellation state holder.
     * @param armorStand        The armor stand.
     */
    public record ArmorStandRenderEvent(@NotNull CancellationState cancellationState,
                                        @NotNull ArmorStandEntity armorStand) implements CancellableEvent {
        /**
         * Creates a new {@link RenderEntityEvents.ArmorStandRenderEvent} suitable for triggering the event.
         * A cached {@link CancellationState#ofCached()} will be used with non-canceled state by default.
         *
         * @param armorStand The armor stand.
         */
        public ArmorStandRenderEvent(@NotNull final ArmorStandEntity armorStand) {
            this(CancellationState.ofCached(), armorStand);
        }
    }
}
