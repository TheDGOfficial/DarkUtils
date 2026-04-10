package gg.darkutils.feat.mining;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.data.PersistentData;
import gg.darkutils.utils.Pair;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.MathUtils;
import gg.darkutils.utils.RoundingMode;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.ScoreboardUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import org.jetbrains.annotations.NotNull;

public final class MineshaftFeatures {
    private static final long MINESHAFT_DESPAWN_TIME = TimeUnit.SECONDS.toNanos(30L);
    private static final long TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);

    @NotNull
    private static final BooleanSupplier IN_GLACITE_TUNNELS =
            TickUtils.queueUpdatingCondition(MineshaftFeatures::isInGlaciteTunnels);

    public static long mineshaftDespawnsAt;

    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
            "  LAPIS CORPSE LOOT! ",  e -> {
                MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.LAPIS);
            },
            "  UMBER CORPSE LOOT! ", e -> {
                MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.UMBER);
            },
            "  TUNGSTEN CORPSE LOOT! ", e -> {
                MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.TUNGSTEN);
            },
            "  VANGUARD CORPSE LOOT! ", e -> {
                MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.VANGUARD);
            },
            "WOW! You found a Glacite Mineshaft portal!", e -> {
                MineshaftFeatures.mineshaftDespawnsAt = System.nanoTime() + MineshaftFeatures.MINESHAFT_DESPAWN_TIME;
            }
    );

    public static long mineshaftEnter;
    public static long activeMiningTimeSinceLastShaft;

    public static double averageSpawnTime;

    private MineshaftFeatures() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(MineshaftFeatures::onChat);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(MineshaftFeatures::onWorldChange);
        TickUtils.queueRepeatingTickTask(MineshaftFeatures::detectMineshaft, 1);

        MineshaftFeatures.updateAverageSpawnTime();
    }

    private static final void detectMineshaft() {
        final var isInShaft = LocationUtils.isInMineshaft();

        if (isInShaft) {
            if (0L == MineshaftFeatures.mineshaftEnter) {
                MineshaftFeatures.mineshaftEnter = System.nanoTime();

                final var timeTook = MineshaftFeatures.activeMiningTimeSinceLastShaft;

                if (0L != timeTook) {
                    MineshaftFeatures.appendTime(timeTook);
                    MineshaftFeatures.activeMiningTimeSinceLastShaft = 0L;
                }
            }
        } else {
            MineshaftFeatures.mineshaftEnter = 0;

            if (ActivityState.isActivelyMining() && LocationUtils.isInDwarvenMines() && MineshaftFeatures.IN_GLACITE_TUNNELS.getAsBoolean()) {
                MineshaftFeatures.activeMiningTimeSinceLastShaft += MineshaftFeatures.TICK_NANOS;
            }
        }
    }

    private static final boolean isInGlaciteTunnels() {
        return ScoreboardUtil.forEachScoreboardLine(line -> {
            if (line.contains("Glacite Tunnels")) {
                return ScoreboardUtil.returning(true);
            }

            return ScoreboardUtil.continuing();
        }, false);
    }

    private static final void appendTime(final long duration) {
        if (null == PersistentData.INSTANCE.timeTookForShafts) {
            PersistentData.INSTANCE.timeTookForShafts = new long[] { duration };
            MineshaftFeatures.averageSpawnTime = duration;
            return;
        }

        final var originalTimeTookValues = PersistentData.INSTANCE.timeTookForShafts;

        final var newLength = originalTimeTookValues.length + 1;

        final var newTimeTookValues = Arrays.copyOf(originalTimeTookValues, newLength);
        newTimeTookValues[newLength - 1] = duration;

        PersistentData.INSTANCE.timeTookForShafts = newTimeTookValues;

        MineshaftFeatures.updateAverageSpawnTime();
    }

    private static final void updateAverageSpawnTime() {
        if (null == PersistentData.INSTANCE.timeTookForShafts) {
            MineshaftFeatures.averageSpawnTime = 0L;
            return;
        }

        MineshaftFeatures.averageSpawnTime = Arrays.stream(PersistentData.INSTANCE.timeTookForShafts)
            .mapToDouble(value -> value)
            .average()
            .orElse(0.0);
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.corpsesPerShaftDisplay && !DarkUtilsConfig.INSTANCE.mineshaftDisplay) {
            return;
        }

        event.match(MineshaftFeatures.MESSAGE_HANDLERS);
    }

    private static final void onWorldChange(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        if (DarkUtilsConfig.INSTANCE.corpsesPerShaftDisplay) {
            MineshaftFeatures.CorpseDataHolder.finalizeAllFound();
        }

        if (DarkUtilsConfig.INSTANCE.mineshaftDisplay) {
            MineshaftFeatures.mineshaftDespawnsAt = 0L;
        }
    }

    private enum CorpseType {
        LAPIS, UMBER, TUNGSTEN, VANGUARD;
    }

    private static final class CorpseDataHolder {
        private static final int[] foundCorpseForType = new int[MineshaftFeatures.CorpseType.values().length];
        private static final int[] lastFoundCorpseForType = new int[MineshaftFeatures.CorpseType.values().length];

        private static final void incrementFound(@NotNull final CorpseType type) {
            ++foundCorpseForType[type.ordinal()];
        }

        private static final void finalizeAllFound() {
            var atLeastOne = false;

            for (var i = 0; i < MineshaftFeatures.CorpseDataHolder.foundCorpseForType.length; ++i) {
                final var foundAmount = MineshaftFeatures.CorpseDataHolder.foundCorpseForType[i];
                if (0 != foundAmount) {
                    MineshaftFeatures.CorpseDataHolder.lastFoundCorpseForType[i] = foundAmount;
                    MineshaftFeatures.CorpseDataHolder.foundCorpseForType[i] = 0;

                    atLeastOne = true;
                }
            }

            if (atLeastOne) {
                MineshaftFeatures.CorpseDataHolder.finalizeShaftFoundCorpses();
            }
        }

        private static final void finalizeShaftFoundCorpses() {
            ++PersistentData.INSTANCE.shaftsEntered;

            final var types = MineshaftFeatures.CorpseType.values();
            for (var i = 0; i < MineshaftFeatures.CorpseDataHolder.lastFoundCorpseForType.length; ++i) {
                final var foundAmount = MineshaftFeatures.CorpseDataHolder.lastFoundCorpseForType[i];

                if (0 != foundAmount) {
                    switch (types[i]) {
                        case LAPIS -> PersistentData.INSTANCE.lapisCorpsesOpened += foundAmount;
                        case UMBER -> PersistentData.INSTANCE.umberCorpsesOpened += foundAmount;
                        case TUNGSTEN -> PersistentData.INSTANCE.tungstenCorpsesOpened += foundAmount;
                        case VANGUARD -> PersistentData.INSTANCE.vanguardCorpsesOpened += foundAmount;
                    }
                }
            }
        }
    }
}

