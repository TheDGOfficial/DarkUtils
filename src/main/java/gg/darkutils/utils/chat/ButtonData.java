package gg.darkutils.utils.chat;

import org.jetbrains.annotations.NotNull;

/**
 * Holds data for a button.
 *
 * @param label   The label of the button.
 * @param hover   The hover text of the button.
 * @param command What command will this button run when clicked.
 */
public record ButtonData(@NotNull String label, @NotNull String hover, @NotNull String command) {
}
