package gg.darkutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.darkutils.DarkUtils;
import gg.darkutils.events.ConfigSaveFinishEvent;
import gg.darkutils.events.ConfigSaveStartEvent;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
import gg.darkutils.utils.LogLevel;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

public final class DarkUtilsConfig {
    private static final @NotNull Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final @NotNull File CONFIG_DIR = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "darkutils"
    );
    private static final @NotNull File NEW_FILE = new File(CONFIG_DIR, "darkutils.json");
    private static final @NotNull File OLD_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "darkutils.json"
    );
    public static final @NotNull DarkUtilsConfig INSTANCE = DarkUtilsConfig.load();

    // === Quality of Life ===
    public boolean autoFishing;
    public boolean autoFishingRecast;
    public boolean autoFishingWorkThroughMenus;
    public int autoFishingStartingDelay = 4;
    public int autoFishingMaximumDelay = 5;
    public boolean neverResetCursorPosition;
    public boolean alwaysSprint;
    public boolean ghostBlockKey;
    public boolean autoTip;
    public boolean disableWelcomeMessage;
    public boolean autoClicker;
    public boolean autoClickerWorkInLevers;
    public boolean autoClickerWorkWithAOTV;
    public boolean disableCellsAlignment;
    public boolean preventUselessBlockHit;
    public boolean disableCommandConfirmation;
    public boolean rejoinCooldownDisplay;
    public boolean laggyServerDetector;
    public boolean vanillaMode;
    public boolean enableModAnnouncer;
    public boolean disableUpdateChecker;

    // === Foraging ===
    public boolean treeGiftConfirmation;
    public boolean treeGiftsPerHour;

    // === Farming ===
    public boolean pestCooldownDisplay;
    public int pestCooldown = 135;
    public boolean persistentTabListWhileFarming;
    public boolean stickyFarmingKeys;
    public boolean stickyForward;
    public boolean stickyBackward;
    public boolean stickyLeft;
    public boolean stickyRight;
    public boolean enforceZorrosCape;

    // === Dungeons ===
    public boolean dialogueSkipTimer;
    public boolean soloCrushTimer;
    public boolean soloCrushWaypoint;
    public boolean autoCloseSecretChests;
    public boolean replaceDiorite;
    public boolean arrowAlignmentDeviceSolver;
    public boolean arrowAlignmentDeviceSolverPredev;
    public boolean arrowAlignmentDeviceSolverBlockIncorrectClicks;
    public boolean arrowStackWaypoints;
    public boolean dungeonTimer;
    public float dungeonTimerScale = 1.0F;
    public int dungeonTimerOffsetX = 0;
    public int dungeonTimerOffsetY = 0;
    public boolean dungeonTimerNoItemIcon;
    public boolean bloodClearedNotification;

    // === Visual Tweaks ===
    public boolean hideEffectsHud;
    public boolean hideEffectsInInventory;
    public boolean transparentPlayerList;
    public boolean removeChatScrollbar;
    public boolean fullbright;
    public boolean nightVision;
    public boolean hideFireOverlay;
    public boolean noBurningEntities;
    public boolean hideArmorAndFood;
    public boolean hideMountHealth;
    public boolean noLightningBolts;
    public boolean noWitherHearts;

    // === Performance ===
    public boolean disableYield;
    public boolean alwaysPrioritizeRenderThread;
    public boolean optimizeExceptions;
    public boolean alwaysUseNoErrorContext;
    public boolean disableErrorCheckingEntirely;
    public boolean reenableAmdGameOptimizations;
    public boolean disableCampfireSmokeParticles;
    public boolean removeMainMenuFrameLimit;
    public boolean logCleaner;
    public boolean stopLightUpdates;
    public boolean noMemoryReserve;
    public boolean optimizeEnumValues;
    @NotNull
    public OpenGLVersionOverride openGLVersionOverride = OpenGLVersionOverride.NO_OVERRIDE;
    public boolean useVirtualThreadsForTextureDownloading;
    public boolean disableGlowing;
    public boolean soundLagFix;
    public boolean threadPriorityTweaker;
    public boolean disableSignatureVerification;
    public boolean blockEntityUnloadLagFix;
    public boolean viewportCache;

    // === Bugfixes ===
    public boolean fixGuiScaleAfterFullscreen;
    public boolean fixInactivityFpsLimiter;
    public boolean itemFrameSoundFix;
    public boolean cursorFix;
    public boolean middleClickFix;
    public boolean cursorPosWaylandGLErrorFix;

    // === Development ===
    @NotNull
    public LogLevel ingameLogLevel = LogLevel.WARN;
    public boolean debugMode;

    private DarkUtilsConfig() {
        super();
    }

    private static final void migrateConfigIfNeeded() {
        if (!DarkUtilsConfig.CONFIG_DIR.exists() && !DarkUtilsConfig.CONFIG_DIR.mkdirs()) {
            DarkUtils.warn(DarkUtilsConfig.class, "Failed to create config directory: " + DarkUtilsConfig.CONFIG_DIR);
        }

        final var oldExists = DarkUtilsConfig.OLD_FILE.exists();
        final var newExists = DarkUtilsConfig.NEW_FILE.exists();

        if (oldExists && !newExists) {
            try {
                Files.move(
                        DarkUtilsConfig.OLD_FILE.toPath(),
                        DarkUtilsConfig.NEW_FILE.toPath(),
                        StandardCopyOption.ATOMIC_MOVE
                );

                DarkUtils.info(
                        DarkUtilsConfig.class,
                        "Migrated config to new location: " + DarkUtilsConfig.NEW_FILE
                );
            } catch (final AtomicMoveNotSupportedException atomicMoveNotSupported) {
                DarkUtils.warn(DarkUtilsConfig.class, "Atomic move is not supported in your filesystem. Falling back to regular move. This is usually fine.");

                try {
                    Files.move(
                            DarkUtilsConfig.OLD_FILE.toPath(),
                            DarkUtilsConfig.NEW_FILE.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    DarkUtils.info(
                            DarkUtilsConfig.class,
                            "Migrated config to new location (non-atomic move): " + DarkUtilsConfig.NEW_FILE
                    );
                } catch (final IOException moveFailure) {
                    DarkUtils.error(DarkUtilsConfig.class, "Failed to migrate config non-atomically", moveFailure);
                    DarkUtils.error(DarkUtilsConfig.class, "Atomic move was not supported due to OS error", atomicMoveNotSupported);
                }
            } catch (final IOException moveFailure) {
                DarkUtils.error(DarkUtilsConfig.class, "Failed to migrate config atomically", moveFailure);
            }
        } else if (oldExists) {
            DarkUtils.warn(
                    DarkUtilsConfig.class,
                    "Both old and new config files exist. Using new location: " + DarkUtilsConfig.NEW_FILE
            );
        }
    }

    private static final @NotNull DarkUtilsConfig load() {
        DarkUtilsConfig.migrateConfigIfNeeded();

        final var configFile = DarkUtilsConfig.NEW_FILE;
        final var configFilePath = configFile.toPath();

        if (configFile.exists()) {
            try {
                return DarkUtilsConfig.GSON.fromJson(Files.readString(configFilePath, StandardCharsets.UTF_8), DarkUtilsConfig.class);
            } catch (final IOException e) {
                DarkUtils.error(DarkUtilsConfig.class, "Unable to load config", e);
                DarkUtils.warn(DarkUtilsConfig.class, "Backing up corrupted config due to load failure as the next save will revert to the default config. If you wish to restore your settings manually, check the file ending with .corrupted extension.");

                // This saves to a separate file with the same name but .corrupted at the end, e.g., config.json.corrupted.
                DarkUtilsConfig.save(configFilePath.resolveSibling(configFilePath.getFileName() + ".corrupted"));
            }
        }
        return new DarkUtilsConfig(); // Use default settings if file does not exist or is corrupted (backed up above already in the latter case)
    }

    static final void save() {
        DarkUtilsConfig.save(DarkUtilsConfig.NEW_FILE.toPath());
    }

    private static final void save(@NotNull final Path path) {
        ConfigSaveStartEvent.INSTANCE.trigger();

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, DarkUtilsConfig.GSON.toJson(DarkUtilsConfig.INSTANCE), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            DarkUtils.error(DarkUtilsConfig.class, "Unable to save config", e);
        } finally {
            ConfigSaveFinishEvent.INSTANCE.trigger();
        }
    }

    @Override
    public final @NotNull String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
    }
}

