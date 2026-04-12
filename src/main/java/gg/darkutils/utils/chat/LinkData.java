package gg.darkutils.utils.chat;

import org.jetbrains.annotations.NotNull;

/**
 * Holds data for a link.
 *
 * @param hover The hover text of the link.
 * @param link  The link.
 */
public record LinkData(@NotNull String hover, @NotNull String link) {
}
