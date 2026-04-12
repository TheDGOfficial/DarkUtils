package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Basic tri-state, since Fabric's one is bloated.
 */
public enum BasicTriState {
    FALSE,
    TRUE,
    DEFAULT;

    private BasicTriState() {
    }

    @NotNull
    public static final BasicTriState of(final boolean state) {
        return state ? BasicTriState.TRUE : BasicTriState.FALSE;
    }
}
