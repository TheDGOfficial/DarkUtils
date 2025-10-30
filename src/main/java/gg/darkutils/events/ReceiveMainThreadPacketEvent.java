package gg.darkutils.events;

import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers before a packet was received by the client from the server on main thread.
 * <p>
 * The server can be an integrated or dedicated server, or a realm.
 * <p>
 * Cancelling will make the game act as if the packet was never received.
 *
 * @param cancellationState The cancellation state holder.
 * @param packet            The packet.
 */
public record ReceiveMainThreadPacketEvent(@NotNull CancellationState cancellationState,
                                           @NotNull Packet<?> packet) implements CancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(ReceiveMainThreadPacketEvent.class);
    }

    /**
     * Creates a new {@link ReceiveMainThreadPacketEvent} suitable for triggering the event.
     * A cached {@link CancellationState#ofCached()} will be used with non-cancelled state by default.
     *
     * @param packet The packet.
     */
    public ReceiveMainThreadPacketEvent(@NotNull final Packet<?> packet) {
        this(CancellationState.ofCached(), packet);
    }
}
