package gg.darkutils.feat.performance;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import org.jetbrains.annotations.NotNull;

public enum OpenGLVersionOverride {
    NO_OVERRIDE(0, 0, "No Override"),
    GL4_0(4, 0, "OpenGL 4.0"),
    GL4_1(4, 1, "OpenGL 4.1"),
    GL4_2(4, 2, "OpenGL 4.2"),
    GL4_3(4, 3, "OpenGL 4.3"),
    GL4_4(4, 4, "OpenGL 4.4"),
    GL4_5(4, 5, "OpenGL 4.5"),
    GL4_6(4, 6, "OpenGL 4.6");

    private final int major;
    private final int minor;

    @NotNull
    private final String prettyName;

    private OpenGLVersionOverride(final int major, final int minor, @NotNull final String prettyName) {
        this.major = major;
        this.minor = minor;

        this.prettyName = prettyName;
    }

    @NotNull
    private static final OpenGLVersionOverride getGLVersionOverride() {
        return DarkUtilsConfig.INSTANCE.openGLVersionOverride;
    }

    public static final int getGLMajorVersion(final int defaultValue) {
        final var override = OpenGLVersionOverride.getGLVersionOverride();

        return OpenGLVersionOverride.NO_OVERRIDE == override ? defaultValue : OpenGLVersionOverride.logGLVersionChange(defaultValue, override.major, true);
    }

    public static final int getGLMinorVersion(final int defaultValue) {
        final var override = OpenGLVersionOverride.getGLVersionOverride();

        return OpenGLVersionOverride.NO_OVERRIDE == override ? defaultValue : OpenGLVersionOverride.logGLVersionChange(defaultValue, override.minor, false);
    }

    private static final int logGLVersionChange(final int oldVersion, final int newVersion, final boolean major) {
        if (newVersion == oldVersion) {
            DarkUtils.info(OpenGLVersionOverride.class, "Not overriding vanilla requested OpenGL {} version of {}", major ? "major" : "minor", oldVersion);
        } else {
            final var baseMessage = "Forcing the game to request OpenGL " + (major ? "major" : "minor") + " version " + newVersion + " instead of " + oldVersion + " during Window context creation.";

            if (newVersion > oldVersion) {
                DarkUtils.info(OpenGLVersionOverride.class, baseMessage);
            } else {
                DarkUtils.warn(OpenGLVersionOverride.class, "{} Downgrading OpenGL version is strongly advised against.", baseMessage);
            }
        }

        return newVersion;
    }

    @NotNull
    public final String prettyName() {
        return this.prettyName;
    }
}
