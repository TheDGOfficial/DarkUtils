package gg.darkutils.events;

import gg.darkutils.events.base.NonCancellableEvent;
import gg.darkutils.events.base.EventRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers when a command message was sent by the local player to the server.
 * <p>
 * A command message is a message that starts with / (the slash).
 * <p>
 * The server can be an integrated or dedicated server, or a realm.
 * <p>
 * This event does not care about the command being a recognized or valid
 * command, having parse errors in arguments, or the user not having permission.
 * <p>
 * Since this not a {@link gg.darkutils.events.base.CancellableEvent}, the server
 * will always receive the command after the event.
 * <p>
 * Therefore, this event shall only be used to monitor sent commands, and not implement
 * any custom commands or add functionality to existing commands.
 *
 * @param command The command.
 */
public record SentCommandEvent(@NotNull String command) implements NonCancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(SentCommandEvent.class);
    }
}
