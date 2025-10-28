package gg.darkutils.events;

import gg.darkutils.events.base.NonCancellableEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers before a packet was received by the client from the server on main thread.
 * <p>
 * The server can be an integrated or dedicated server, or a realm.
 *
 * @param packet The packet.
 */
public record ReceiveMainThreadPacketEvent(@NotNull Packet<?> packet) implements NonCancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(ReceiveMainThreadPacketEvent.class);
    }
}
