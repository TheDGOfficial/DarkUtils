package gg.darkutils.feat.mining;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.data.PersistentData;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.PrettyUtils;
import gg.darkutils.utils.ScoreboardUtil;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class MineshaftFeatures {
    private static final long MINESHAFT_DESPAWN_TIME = TimeUnit.SECONDS.toNanos(30L);
    private static final long TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);

    @NotNull
    private static final BooleanSupplier IN_GLACITE_TUNNELS =
            TickUtils.queueUpdatingCondition(MineshaftFeatures::isInGlaciteTunnels);

    static long mineshaftDespawnsAt;
    static long mineshaftEnter;
    static long activeMiningTimeSinceLastShaftSpawn;
    static double averageSpawnTime;
    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
            "  LAPIS CORPSE LOOT! ", e -> MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.LAPIS),
            "  UMBER CORPSE LOOT! ", e -> MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.UMBER),
            "  TUNGSTEN CORPSE LOOT! ", e -> MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.TUNGSTEN),
            "  VANGUARD CORPSE LOOT! ", e -> MineshaftFeatures.CorpseDataHolder.incrementFound(MineshaftFeatures.CorpseType.VANGUARD),
            "WOW! You found a Glacite Mineshaft portal!", e -> MineshaftFeatures.onShaftSpawn()
    );
    static double averageTimeInShaft;
    private static long timeSinceShaftEnter;

    private MineshaftFeatures() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(MineshaftFeatures::onChat);
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(MineshaftFeatures::onWorldChange);
        TickUtils.queueRepeatingTickTask(MineshaftFeatures::detectMineshaft, 1);

        MineshaftFeatures.updateAverageSpawnTime();
        MineshaftFeatures.updateAverageSpentTime();
    }

    private static final void onShaftSpawn() {
        MineshaftFeatures.mineshaftDespawnsAt = System.nanoTime() + MineshaftFeatures.MINESHAFT_DESPAWN_TIME;

        final var timeTook = MineshaftFeatures.activeMiningTimeSinceLastShaftSpawn;

        if (0L != timeTook) {
            MineshaftFeatures.appendSpawnTime(timeTook);
            MineshaftFeatures.activeMiningTimeSinceLastShaftSpawn = 0L;

            if (DarkUtilsConfig.INSTANCE.mineshaftDisplay) {
                DarkUtils.user("Mineshaft took " + PrettyUtils.prettifyNanosToSeconds(timeTook) + " to spawn.", DarkUtils.UserMessageLevel.USER_INFO);
            }
        }
    }

    private static final void detectMineshaft() {
        final var isInShaft = LocationUtils.isInMineshaft();

        if (isInShaft) {
            if (0L == MineshaftFeatures.mineshaftEnter) {
                MineshaftFeatures.mineshaftEnter = System.nanoTime();
                MineshaftFeatures.timeSinceShaftEnter = 0L;
            } else {
                MineshaftFeatures.timeSinceShaftEnter += MineshaftFeatures.TICK_NANOS;
            }
        } else {
            MineshaftFeatures.mineshaftEnter = 0L;

            final var timeInShaft = MineshaftFeatures.timeSinceShaftEnter;
            if (0L != timeInShaft) {
                MineshaftFeatures.appendSpentTime(timeInShaft);
                MineshaftFeatures.timeSinceShaftEnter = 0L;
            }

            if (ActivityState.isActivelyMining() && LocationUtils.isInDwarvenMines() && MineshaftFeatures.IN_GLACITE_TUNNELS.getAsBoolean()) {
                MineshaftFeatures.activeMiningTimeSinceLastShaftSpawn += MineshaftFeatures.TICK_NANOS;
            }
        }
    }

    private static final boolean isInGlaciteTunnels() {
        if (!LocationUtils.isInDwarvenMines()) {
            return false;
        }

        for (final var line : ScoreboardUtil.scoreboardLines()) {
            if (line.contains("Glacite Tunnels")) {
                return true;
            }
        }

        return false;
    }

    private static final void appendSpawnTime(final long duration) {
        if (null == PersistentData.INSTANCE.timeTookForShafts) {
            PersistentData.INSTANCE.timeTookForShafts = new long[]{duration};
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
        final var times = PersistentData.INSTANCE.timeTookForShafts;

        if (null == times) {
            MineshaftFeatures.averageSpawnTime = 0.0D;
            return;
        }

        MineshaftFeatures.averageSpawnTime = Arrays.stream(times)
                .mapToDouble(value -> value)
                .average()
                .orElse(0.0);
    }

    private static final void appendSpentTime(final long duration) {
        if (null == PersistentData.INSTANCE.timeSpentInShafts) {
            PersistentData.INSTANCE.timeSpentInShafts = new long[]{duration};
            MineshaftFeatures.averageTimeInShaft = duration;
            return;
        }

        final var originalTimeSpentValues = PersistentData.INSTANCE.timeSpentInShafts;

        final var newLength = originalTimeSpentValues.length + 1;

        final var newTimeSpentValues = Arrays.copyOf(originalTimeSpentValues, newLength);
        newTimeSpentValues[newLength - 1] = duration;

        PersistentData.INSTANCE.timeSpentInShafts = newTimeSpentValues;

        MineshaftFeatures.updateAverageSpentTime();
    }

    private static final void updateAverageSpentTime() {
        final var times = PersistentData.INSTANCE.timeSpentInShafts;

        if (null == times) {
            MineshaftFeatures.averageTimeInShaft = 0.0D;
            return;
        }

        MineshaftFeatures.averageTimeInShaft = Arrays.stream(times)
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

    private static final void onWorldChange(@NotNull final Minecraft client, @NotNull final ClientLevel world) {
        if (DarkUtilsConfig.INSTANCE.corpsesPerShaftDisplay) {
            MineshaftFeatures.CorpseDataHolder.finalizeAllFound();
        }

        if (DarkUtilsConfig.INSTANCE.mineshaftDisplay) {
            MineshaftFeatures.mineshaftDespawnsAt = 0L;
        }
    }

    private enum CorpseType {
        LAPIS(v -> PersistentData.INSTANCE.lapisCorpsesOpened += v),
        UMBER(v -> PersistentData.INSTANCE.umberCorpsesOpened += v),
        TUNGSTEN(v -> PersistentData.INSTANCE.tungstenCorpsesOpened += v),
        VANGUARD(v -> PersistentData.INSTANCE.vanguardCorpsesOpened += v);

        private final @NonNull IntConsumer incrementer;

        private CorpseType(final @NonNull IntConsumer incrementer) {
            this.incrementer = incrementer;
        }

        private final void increment(final int amount) {
            this.incrementer.accept(amount);
        }
    }

    private static final class CorpseDataHolder {
        private static final int @NonNull [] foundCorpseForType = new int[MineshaftFeatures.CorpseType.values().length];

        private CorpseDataHolder() {
            super();

            throw new UnsupportedOperationException("static-only class");
        }

        private static final void incrementFound(@NotNull final MineshaftFeatures.CorpseType type) {
            ++MineshaftFeatures.CorpseDataHolder.foundCorpseForType[type.ordinal()];
        }

        private static final void finalizeAllFound() {
            var atLeastOne = false;

            final var types = MineshaftFeatures.CorpseType.values();
            for (int i = 0, len = MineshaftFeatures.CorpseDataHolder.foundCorpseForType.length; i < len; ++i) {
                final var foundAmount = MineshaftFeatures.CorpseDataHolder.foundCorpseForType[i];
                if (0 != foundAmount) {
                    types[i].increment(foundAmount);
                    MineshaftFeatures.CorpseDataHolder.foundCorpseForType[i] = 0;

                    atLeastOne = true;
                }
            }

            if (atLeastOne) {
                ++PersistentData.INSTANCE.shaftsEntered;
            }
        }
    }
}

