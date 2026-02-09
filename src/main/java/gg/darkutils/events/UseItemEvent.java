package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers before an item has been used.
 * <p>
 * Cancelling will make the game act as if the item was never used.
 *
 * @param cancellationState The cancellation state holder.
 * @param itemStack         The item that is going to be used if not canceled.
 */
public record UseItemEvent(@NotNull CancellationState cancellationState,
                           @NotNull ItemStack itemStack) implements CancellableEvent {
    /**
     * Creates a new {@link UseItemEvent} suitable for triggering the event.
     * A fresh {@link CancellationState#ofFresh()} will be used with non-canceled state by default.
     *
     * @param itemStack The item that is going to be used if not canceled.
     */
    public UseItemEvent(@NotNull final ItemStack itemStack) {
        this(CancellationState.ofFresh(), itemStack);
    }
}
