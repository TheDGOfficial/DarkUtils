package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Basic tri-state, since Fabric's one is bloated.
 */
public enum BasicTriState {
    FALSE,
    TRUE,
    DEFAULT;

    @NotNull
    public static final BasicTriState of(final boolean b) {
        return b ? BasicTriState.TRUE : BasicTriState.FALSE;
    }

    private BasicTriState() {
    }
}
