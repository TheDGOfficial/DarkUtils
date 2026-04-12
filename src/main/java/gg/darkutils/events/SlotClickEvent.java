package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers after the player clicks on a slot inside an inventory but not yet sent to the server.
 * <p>
 * Cancelling will make the game act as if the click never happened.
 *
 * @param cancellationState The cancellation state holder.
 * @param handledScreen     The handled screen the slot was clicked in.
 * @param slotId            The slot id that was clicked.
 * @param slot              The slot.
 */
public record SlotClickEvent(@NotNull CancellationState cancellationState,
                             @NotNull AbstractContainerScreen<?> handledScreen,
                             int slotId,
                             @NotNull Slot slot) implements CancellableEvent {
    /**
     * Creates a new {@link SlotClickEvent} suitable for triggering the event.
     * A fresh {@link CancellationState#ofFresh()} will be used with non-canceled state by default.
     *
     * @param handledScreen The handled screen the slot was clicked in.
     * @param slotId        The slot id that was clicked.
     * @param slot          The slot.
     */
    public SlotClickEvent(@NotNull final AbstractContainerScreen<?> handledScreen, final int slotId, @NotNull final Slot slot) {
        this(CancellationState.ofFresh(), handledScreen, slotId, slot);
    }
}
