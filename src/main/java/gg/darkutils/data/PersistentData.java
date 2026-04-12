package gg.darkutils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.darkutils.DarkUtils;
import gg.darkutils.utils.JavaUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class PersistentData {
    public static final @NotNull PersistentData INSTANCE;
    private static final @NotNull Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final @NotNull File DATA_DIR = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "darkutils"
    );
    private static final @NotNull File FILE = new File(
            PersistentData.DATA_DIR,
            "persistent_data.json"
    );
    private static final @NotNull Path TEMP_FILE =
            PersistentData.FILE.toPath().resolveSibling(PersistentData.FILE.getName() + ".tmp");
    private static @Nullable String lastSavedJson;

    static {
        PersistentData.cleanupTempFile();

        INSTANCE = PersistentData.load();
        PersistentData.lastSavedJson = PersistentData.GSON.toJson(PersistentData.INSTANCE);

        Runtime.getRuntime().addShutdownHook(
                Thread.ofPlatform()
                        .name("DarkUtils PersistentData Shutdown Saver")
                        .unstarted(() -> {
                            try {
                                PersistentData.saveAtomicIfDirty();
                            } catch (final Throwable error) {
                                throw JavaUtils.sneakyThrow(error);
                            }
                        })
        );
    }

    // === Persistent Fields ===

    // === Farming ===
    public boolean zorrosCapeEquipped;

    // === Mining ===
    public int shaftsEntered;
    public int lapisCorpsesOpened;
    public int umberCorpsesOpened;
    public int tungstenCorpsesOpened;
    public int vanguardCorpsesOpened;
    public long @Nullable [] timeTookForShafts;
    public long @Nullable [] timeSpentInShafts;

    // ================================

    private PersistentData() {
        super();
    }

    private static final @NotNull PersistentData load() {
        try {
            Files.createDirectories(PersistentData.DATA_DIR.toPath());
        } catch (final IOException e) {
            DarkUtils.error(PersistentData.class, "Failed to create persistent data directory", e);
        }

        if (PersistentData.FILE.exists()) {
            try {
                return PersistentData.GSON.fromJson(
                        Files.readString(PersistentData.FILE.toPath(), StandardCharsets.UTF_8),
                        PersistentData.class
                );
            } catch (final IOException e) {
                DarkUtils.error(PersistentData.class, "Unable to load persistent data", e);

                try {
                    Files.move(
                            PersistentData.FILE.toPath(),
                            PersistentData.FILE.toPath().resolveSibling(PersistentData.FILE.getName() + ".corrupted"),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (final IOException e2) {
                    DarkUtils.error(PersistentData.class, "Failed to backup corrupted persistent data file", e2);
                }
            }
        }

        return new PersistentData();
    }

    public static final void save() {
        PersistentData.save(PersistentData.FILE.toPath());
    }

    private static final void save(@NotNull final Path path) {
        try {
            Files.createDirectories(path.getParent());

            final var json = PersistentData.GSON.toJson(PersistentData.INSTANCE);

            Files.writeString(
                    path,
                    json,
                    StandardCharsets.UTF_8
            );

            PersistentData.lastSavedJson = json;
        } catch (final IOException e) {
            DarkUtils.error(PersistentData.class, "Unable to save persistent data", e);
        }
    }

    private static final void saveAtomicIfDirty() {
        final var lastJson = PersistentData.lastSavedJson;
        final var json = PersistentData.GSON.toJson(PersistentData.INSTANCE);

        if (!Objects.equals(lastJson, json)) {
            PersistentData.saveAtomic(json);
        }
    }

    public static final void saveAtomic() {
        PersistentData.saveAtomic(PersistentData.FILE.toPath());
    }

    private static final void saveAtomic(@NotNull final Path path) {
        PersistentData.saveAtomic(path, PersistentData.GSON.toJson(PersistentData.INSTANCE));
    }

    private static final void saveAtomic(@NotNull final String json) {
        PersistentData.saveAtomic(PersistentData.FILE.toPath(), json);
    }

    private static final void saveAtomic(@NotNull final Path path, @NotNull final String json) {
        try {
            Files.createDirectories(path.getParent());

            final var tempFile = PersistentData.TEMP_FILE;

            Files.writeString(tempFile, json, StandardCharsets.UTF_8);

            try {
                Files.move(
                        tempFile,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (final AtomicMoveNotSupportedException ignored) {
                DarkUtils.warn(PersistentData.class, "Atomic move is not supported, falling back to regular move. This is usually normal.");
                Files.move(
                        tempFile,
                        path,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            PersistentData.lastSavedJson = json;
        } catch (final IOException e) {
            DarkUtils.error(PersistentData.class, "Failed to save persistent data", e);
        }
    }

    private static final void cleanupTempFile() {
        final var tempFile = PersistentData.TEMP_FILE;

        try {
            if (Files.deleteIfExists(tempFile)) {
                DarkUtils.warn(
                        PersistentData.class,
                        "Removed leftover temporary persistent data file automatically: " + tempFile
                                + ". You don't have to do anything if this was after a power loss. "
                                + "If it happens consistently please report this."
                );
            }
        } catch (final IOException e) {
            DarkUtils.error(
                    PersistentData.class,
                    "Failed to remove leftover temporary persistent data file: " + tempFile
                            + ". This will not cause any issues but is still an error condition.",
                    e
            );
        }
    }

    @Override
    public final @NotNull String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
    }
}
