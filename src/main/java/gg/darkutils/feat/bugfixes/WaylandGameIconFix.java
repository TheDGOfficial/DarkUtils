package gg.darkutils.feat.bugfixes;

import gg.darkutils.utils.OSUtils;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class WaylandGameIconFix {
    public static final @NotNull String WAYLAND_APP_ID = "com.mojang.minecraft.java-edition";
    public static boolean desktopFileFailure;
    private static final @NotNull String DESKTOP_FILE_NAME = WaylandGameIconFix.WAYLAND_APP_ID + ".desktop";
    private static final @NotNull String ICON_FILE_NAME = "minecraft.png";

    private WaylandGameIconFix() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    @Nullable
    private static final Path userHomePath() {
        final var home = OSUtils.getEnvironmentVariable("HOME");

        if (null != home) {
            return Paths.get(home);
        }

        final var fallback = System.getProperty("user.home");

        return null == fallback || fallback.isEmpty()
                ? null
                : Paths.get(fallback);
    }

    @Nullable
    private static final Path userDataPath() {
        final var home = WaylandGameIconFix.userHomePath();

        if (null != home) {
            // Intended to be used over XDG_DATA_HOME for PrismLauncher flatpak version compatibility (PrismLauncher sets XDG_DATA_HOME to /home/user/.var/app/org.prismlauncher.PrismLauncher/data/, which KDE/GNOME won't read desktop files or icons from)
            return home.resolve(".local/share/");
        }

        final var xdgDataHome = OSUtils.getEnvironmentVariable("XDG_DATA_HOME");
        return null == xdgDataHome ? null : Paths.get(xdgDataHome);
    }

    @Nullable
    private static final Path desktopFilePath() {
        final var userDataPath = WaylandGameIconFix.userDataPath();
        return null == userDataPath ? null : userDataPath.resolve("applications").resolve(WaylandGameIconFix.DESKTOP_FILE_NAME);
    }

    @Nullable
    private static final Path hicolorThemePath() {
        final var userDataPath = WaylandGameIconFix.userDataPath();
        return null == userDataPath ? null : userDataPath
                                             .resolve("icons")
                                             .resolve("hicolor");
    }

    @Nullable
    private static final Path iconFilePath(final int width, final int height) {
        final var hicolorThemePath = WaylandGameIconFix.hicolorThemePath();
        return null == hicolorThemePath ? null : hicolorThemePath
                                                 .resolve(width + "x" + height)
                                                 .resolve("apps")
                                                 .resolve(WaylandGameIconFix.ICON_FILE_NAME);
    }

    private static final void writeTemporaryFileData(@NotNull final Path path, final byte @NonNull ... data) throws IOException {
        final var parent = path.getParent();

        if (null != parent) {
            Files.createDirectories(parent);
        }

        Files.write(path, data);
        path.toFile().deleteOnExit();
    }

    public static final void generateDesktopFile() throws IOException {
        final var desktopFilePath = WaylandGameIconFix.desktopFilePath();

        if (null == desktopFilePath) {
            return;
        }

        WaylandGameIconFix.writeTemporaryFileData(desktopFilePath,
                """
                        [Desktop Entry]
                        Name=Minecraft
                        Comment=Explore your own unique world, survive the night, and create anything you can imagine!
                        Icon=minecraft
                        Type=Application
                        NoDisplay=true
                        Categories=Game;
                        """
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public static final void setIcon(@NotNull final List<IoSupplier<InputStream>> icons) throws IOException {
        for (final var ioSupplier : icons) {
            try (final var icon = ioSupplier.get()) {
                final var data = IOUtils.toByteArray(icon);
                final var image = ImageIO.read(new ByteArrayInputStream(data));

                if (null == image) {
                    continue;
                }

                final var path = WaylandGameIconFix.iconFilePath(image.getWidth(), image.getHeight());

                if (null == path) {
                    continue;
                }

                WaylandGameIconFix.writeTemporaryFileData(path, data);
            }
        }

        WaylandGameIconFix.updateIconCache();
    }

    private static final void updateIconCache() throws IOException {
        final var currentDesktop = OSUtils.getEnvironmentVariable("XDG_CURRENT_DESKTOP");

        if (null == currentDesktop) {
            return;
        }

        final var kde = currentDesktop.contains("KDE");

        var processCmdline = new String[]{"xdg-icon-resource", "forceupdate"};

        if (kde) {
            processCmdline = new String[]{"dbus-send", "--session", "/KGlobalSettings", "org.kde.KGlobalSettings.notifyChange", "int32:0", "int32:0"};
        } else if (currentDesktop.contains("GNOME")) {
            final var hicolorThemePath = WaylandGameIconFix.hicolorThemePath();
            processCmdline = null == hicolorThemePath ? new String[]{"gtk-update-icon-cache"} : new String[]{"gtk-update-icon-cache", hicolorThemePath.toString()};
        }

        OSUtils.runCommand(processCmdline);

        if (kde) {
            OSUtils.runCommand(
                    "dbus-send",
                    "--session",
                    "/KIconLoader",
                    "org.kde.KIconLoader.iconChanged",
                    "int32:0",
                    "int32:0"
            );
        }
    }
}
