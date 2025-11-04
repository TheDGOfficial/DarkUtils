package gg.darkutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.darkutils.DarkUtils;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
import gg.darkutils.utils.LogLevel;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class DarkUtilsConfig {
    private static final @NotNull Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final @NotNull File FILE = new File(
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
    public boolean welcomeMessage;
    public boolean autoClicker;
    public boolean autoClickerWorkInLevers;
    public boolean disableCellsAlignment;
    public boolean preventUselessBlockHit;
    public boolean disableCommandConfirmation;
    public boolean rejoinCooldownDisplay;
    public boolean laggyServerDetector;

    // === Foraging ===
    public boolean treeGiftConfirmation;
    public boolean treeGiftsPerHour;

    // === Dungeons ===
    public boolean dialogueSkipTimer;
    public boolean soloCrushTimer;
    public boolean autoCloseSecretChests;
    public boolean replaceDiorite;
    public boolean arrowAlignmentDeviceSolver;
    public boolean arrowAlignmentDeviceSolverPredev;
    public boolean arrowAlignmentDeviceSolverBlockIncorrectClicks;

    // === Visual Tweaks ===
    public boolean hideEffectsHud;
    public boolean hideEffectsInInventory;
    public boolean transparentScoreboard;
    public boolean transparentNametags;
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
    public boolean armorStandOptimizer;
    public int armorStandLimit = 50;
    public boolean disableYield;
    public boolean alwaysPrioritizeRenderThread;
    public boolean optimizeExceptions;
    public boolean alwaysUseNoErrorContext;
    public boolean disableCampfireSmokeParticles;
    public boolean removeMainMenuFrameLimit;
    public boolean logCleaner;
    public boolean stopLightUpdates;
    public boolean noMemoryReserve;
    @NotNull
    public OpenGLVersionOverride openGLVersionOverride = OpenGLVersionOverride.NO_OVERRIDE;
    public boolean useVirtualThreadsForTextureDownloading;
    public boolean disableGlowing;
    public boolean soundLagFix;
    public boolean threadPriorityTweaker;

    // === Bugfixes ===
    public boolean fixGuiScaleAfterFullscreen;
    public boolean fixInactivityFpsLimiter;

    // === Development ===
    @NotNull
    public LogLevel ingameLogLevel = LogLevel.WARN;

    private DarkUtilsConfig() {
        super();
    }

    private static final @NotNull DarkUtilsConfig load() {
        if (DarkUtilsConfig.FILE.exists()) {
            try {
                return DarkUtilsConfig.GSON.fromJson(Files.readString(DarkUtilsConfig.FILE.toPath(), StandardCharsets.UTF_8), DarkUtilsConfig.class);
            } catch (final IOException e) {
                DarkUtils.error(DarkUtilsConfig.class, "Unable to load config", e);
            }
        }
        return new DarkUtilsConfig();
    }

    static final void save() {
        try {
            Files.writeString(DarkUtilsConfig.FILE.toPath(), DarkUtilsConfig.GSON.toJson(DarkUtilsConfig.INSTANCE), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            DarkUtils.error(DarkUtilsConfig.class, "Unable to save config", e);
        }
    }
}

