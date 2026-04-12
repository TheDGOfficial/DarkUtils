package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

public enum LogLevel {
    INFO("Info"),
    WARN("Warning"),
    ERROR("Error");

    @NotNull
    private final String prettyName;

    private LogLevel(@NotNull final String prettyName) {
        this.prettyName = prettyName;
    }

    @NotNull
    public final String prettyName() {
        return this.prettyName;
    }
}
