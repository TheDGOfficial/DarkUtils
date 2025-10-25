package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers before an item has been used.
 * <p>
 * Cancelling will make the game act as if the item was never used.
 *
 * @param cancellationState The cancellation state holder.
 * @param itemStack         The item that is going to be used if not cancelled.
 */
public record UseItemEvent(@NotNull CancellationState cancellationState,
                           @NotNull ItemStack itemStack) implements CancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(UseItemEvent.class);
    }

    /**
     * Creates a new {@link UseItemEvent} suitable for triggering the event.
     * A cached {@link CancellationState#ofCached()} will be used with non-cancelled state by default.
     *
     * @param itemStack The item that is going to be used if not cancelled.
     */
    public UseItemEvent(@NotNull final ItemStack itemStack) {
        this(CancellationState.ofCached(), itemStack);
    }
}
