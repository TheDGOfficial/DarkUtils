package gg.darkutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.darkutils.DarkUtils;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
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
    public boolean neverResetCursorPosition;
    public boolean alwaysSprint;
    public boolean ghostBlockKey;
    public boolean autoTip;
    public boolean welcomeMessage;

    // === Foraging ===
    public boolean treeGiftConfirmation;
    public boolean treeGiftsPerHour;

    // === Dungeons ===
    public boolean dialogueSkipTimer;
    public boolean soloCrushTimer;
    public boolean autoCloseSecretChests;
    public boolean replaceDiorite;

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

    // === Bugfixes ===
    public boolean fixGuiScaleAfterFullscreen;
    public boolean fixInactivityFpsLimiter;

    private DarkUtilsConfig() {
        super();
    }

    private static final @NotNull DarkUtilsConfig load() {
        if (DarkUtilsConfig.FILE.exists()) {
            try (final var reader = Files.newBufferedReader(DarkUtilsConfig.FILE.toPath(), StandardCharsets.UTF_8)) {
                return DarkUtilsConfig.GSON.fromJson(reader, DarkUtilsConfig.class);
            } catch (final IOException e) {
                DarkUtils.LOGGER.error("Unable to load config", e);
            }
        }
        return new DarkUtilsConfig();
    }

    static final void save() {
        try (final var writer = Files.newBufferedWriter(DarkUtilsConfig.FILE.toPath(), StandardCharsets.UTF_8)) {
            DarkUtilsConfig.GSON.toJson(DarkUtilsConfig.INSTANCE, writer);
        } catch (final IOException e) {
            DarkUtils.LOGGER.error("Unable to save config", e);
        }
    }
}

