package gg.darkutils.events;

import gg.darkutils.events.base.NonCancellableEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers when a message was sent by the local player to the server.
 * <p>
 * The server can be an integrated or dedicated server, or a realm.
 *
 * @param content The message content.
 */
public record SentMessageEvent(@NotNull String content) implements NonCancellableEvent {
}
