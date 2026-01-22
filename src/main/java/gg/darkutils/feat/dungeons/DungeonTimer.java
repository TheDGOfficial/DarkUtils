package gg.darkutils.feat.dungeons;

import gg.darkutils.DarkUtils;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.BasicColor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class DungeonTimer {
    private static final long TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);
    private static final long MIN_DISPLAY_LAG_NANO = TimeUnit.MILLISECONDS.toNanos(100L);
    private static final long SECONDS_PER_DAY = TimeUnit.DAYS.toSeconds(1L);
    private static final long SECONDS_PER_HOUR = TimeUnit.HOURS.toSeconds(1L);
    private static final long SECONDS_PER_MINUTE = TimeUnit.MINUTES.toSeconds(1L);
    private static final long HUNDRED_MS_AS_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);
    @NotNull
    private static final ArrayList<DungeonTimer.RenderableLine> lines = new ArrayList<>(32);
    private static long serverTickCounter;
    @NotNull
    private static final Consumer<ReceiveGameMessageEvent> PHASE_5_FINISH = event -> {
        if (event.isStyledWith(BasicColor.DARK_RED)) {
            DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_5_CLEAR);
        }
    };
    private static long lastClientNow;
    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.ofEntries(
            Map.entry("Starting in 1 second.", event -> {
                if (event.isStyledWith(BasicColor.GREEN)) {
                    TickUtils.queueServerTickTask(() -> DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.DUNGEON_START), 20);
                }
            }),
            Map.entry("The BLOOD DOOR has been opened!", event -> {
                if (event.isStyledWith(BasicColor.RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.BLOOD_OPEN);
                }
            }),
            Map.entry("[BOSS] The Watcher: You have proven yourself. You may pass.", event -> {
                if (event.isStyledWith(BasicColor.RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.BLOOD_CLEAR);

                    TickUtils.queueTickTask(() -> {
                        final var seconds = DungeonTimer.getPhaseTimeInSecondsForPhase(DungeonTimer.DungeonPhase.BLOOD_OPEN, DungeonTimer.DungeonPhase.BLOOD_CLEAR, false);
                        Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, "§cBlood Cleared! (" + seconds + "s)", 60);
                    }, 1);
                }
            }),
            Map.entry("[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!", event -> {
                if (event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.BOSS_ENTRY);
                }
            }),
            Map.entry("[BOSS] Storm: Pathetic Maxor, just like expected.", event -> {
                if (event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_1_CLEAR);
                }
            }),
            Map.entry("[BOSS] Goldor: Who dares trespass into my domain?", event -> {
                if (event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_2_CLEAR);
                }
            }),
            Map.entry("The Core entrance is opening!", event -> {
                if (event.isStyledWith(BasicColor.GREEN)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.TERMINALS_CLEAR);
                }
            }),
            Map.entry("[BOSS] Necron: You went further than any human before, congratulations.", event -> {
                if (event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_3_CLEAR);
                }
            }),
            Map.entry("[BOSS] Necron: All this, for nothing...", event -> {
                if (event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_4_CLEAR);
                }
            }),
            Map.entry("[BOSS] Wither King: Incredible. You did what I couldn't do myself.", DungeonTimer.PHASE_5_FINISH),
            Map.entry("[BOSS] Wither King: Thank you for coming all the way here.", DungeonTimer.PHASE_5_FINISH)
    );
    private static long lastServerTickNow;
    private static long lastMonotonicGlobalLagNano;
    private static int linesSize;
    private static boolean skipRender = true;

    private DungeonTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final boolean isPhaseFinished(@NotNull final DungeonTimer.DungeonPhase phase) {
        return null != DungeonTimer.DungeonTimingState.getPhase(phase);
    }

    public static final boolean isPhaseNotFinished(@NotNull final DungeonTimer.DungeonPhase phase) {
        return null == DungeonTimer.DungeonTimingState.getPhase(phase);
    }

    public static final boolean isInBetweenPhases(@NotNull final DungeonTimer.DungeonPhase started, @NotNull final DungeonTimer.DungeonPhase notYetFinished) {
        return DungeonTimer.isPhaseFinished(started) && DungeonTimer.isPhaseNotFinished(notYetFinished);
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(DungeonTimer::onChat);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(DungeonTimer::reset);

        TickUtils.queueRepeatingServerTickTask(() -> ++DungeonTimer.serverTickCounter, 1);

        TickUtils.queueRepeatingTickTask(DungeonTimer::updateDungeonTimer, 1);
        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "dungeon_timer"), (context, tickCounter) -> DungeonTimer.renderDungeonTimer(context));
    }

    private static final long getPhaseTime(final long prevPhaseTime, final long targetPhaseTime, final boolean live) {
        if (0L == prevPhaseTime) {
            return 0L;
        }

        final var effectiveTargetPhaseTime =
                live && 0L == targetPhaseTime
                        ? DungeonTimer.lastClientNow
                        : targetPhaseTime;

        return Math.max(0L, effectiveTargetPhaseTime - prevPhaseTime);
    }

    private static final long getPhaseTimeInSecondsForPhase(@NotNull final DungeonTimer.DungeonPhase start, @NotNull final DungeonTimer.DungeonPhase end, final boolean live) {
        final var startTime = DungeonTimer.DungeonTimingState.getPhase(start);

        if (null == startTime) {
            return 0L;
        }

        final var endTime = DungeonTimer.DungeonTimingState.getPhase(end);
        final var endClientNano = null == endTime ? 0L : endTime.clientNano;

        return DungeonTimer.getPhaseTimeInSeconds(startTime.clientNano, endClientNano, live);
    }

    private static final long getPhaseTimeInSeconds(final long prevPhaseTime, final long targetPhaseTime, final boolean live) {
        return TimeUnit.NANOSECONDS.toSeconds(DungeonTimer.getPhaseTime(prevPhaseTime, targetPhaseTime, live));
    }

    private static final long getPhaseLagTimeInNanos(@Nullable final DungeonTimer.PhaseTiming startTiming, @Nullable final DungeonTimer.PhaseTiming endTiming) {
        if (null == startTiming) {
            return 0L;
        }

        final var endLagNano =
                null == endTiming
                        ? DungeonTimer.lastMonotonicGlobalLagNano
                        : endTiming.globalLagNanoAtFinish;

        final var lagNano =
                Math.max(0L, endLagNano - startTiming.globalLagNanoAtFinish);

        // LIVE phase: monotonic clamp in nanos
        if (null == endTiming) {
            startTiming.maxLagNano = Math.max(startTiming.maxLagNano, lagNano);
            return startTiming.maxLagNano;
        }

        // FINISHED phase: exact
        return lagNano;
    }

    @NotNull
    private static final String formatSeconds(final long seconds) {
        if (60L > seconds) {
            return seconds + "s";
        }

        var remainingSeconds = seconds;

        final var days = remainingSeconds / DungeonTimer.SECONDS_PER_DAY;
        remainingSeconds -= days * DungeonTimer.SECONDS_PER_DAY;

        final var hours = remainingSeconds / DungeonTimer.SECONDS_PER_HOUR;
        remainingSeconds -= hours * DungeonTimer.SECONDS_PER_HOUR;

        final var minutes = remainingSeconds / DungeonTimer.SECONDS_PER_MINUTE;
        remainingSeconds -= minutes * DungeonTimer.SECONDS_PER_MINUTE;

        final var builder = new StringBuilder(8);
        var needsSpace = false;

        if (0L < days) {
            builder.append(days).append('d');
            needsSpace = true;
        }

        if (0L < hours) {
            if (needsSpace) {
                builder.append(' ');
            }
            builder.append(hours).append('h');
            needsSpace = true;
        }

        if (0L < minutes) {
            if (needsSpace) {
                builder.append(' ');
            }
            builder.append(minutes).append('m');
            needsSpace = true;
        }

        if (0L < remainingSeconds || !needsSpace) {
            if (needsSpace) {
                builder.append(' ');
            }
            builder.append(remainingSeconds).append('s');
        }

        return builder.toString();
    }

    @NotNull
    private static final String formatNanosAsSeconds(final long nanos) {
        if (0L >= nanos) {
            return "0s";
        }

        // truncate to 1 decimal (no rounding up - monotonic-safe)
        final var secondsTimes10 = nanos / DungeonTimer.HUNDRED_MS_AS_NANOS;

        final var wholeSeconds = secondsTimes10 / 10L;

        if (60L > wholeSeconds) {
            final var decimal = secondsTimes10 % 10L;
            return 0L == decimal
                    ? wholeSeconds + "s"
                    : wholeSeconds + "." + decimal + 's';
        }

        // fall back to existing formatter for large values
        return DungeonTimer.formatSeconds(wholeSeconds);
    }

    private static final void addLine(@NotNull final DungeonTimer.DungeonPhase start, @NotNull final DungeonTimer.DungeonPhase end, @NotNull final String prettyName, @NotNull final Formatting color, @Nullable final Item optionalItemIcon) {
        final var startTime = DungeonTimer.DungeonTimingState.getPhase(start);

        if (null == startTime) {
            return;
        }

        final var endTime = DungeonTimer.DungeonTimingState.getPhase(end);
        final var endClientNano = null == endTime ? 0L : endTime.clientNano;

        final var phaseTime = DungeonTimer.getPhaseTimeInSeconds(startTime.clientNano, endClientNano, true);

        if (0L == phaseTime) {
            return;
        }

        final var lagNano = DungeonTimer.getPhaseLagTimeInNanos(startTime, endTime);

        final var text = DungeonTimer.MIN_DISPLAY_LAG_NANO <= lagNano
                ? prettyName + ": " + DungeonTimer.formatSeconds(phaseTime)
                + " (-" + DungeonTimer.formatNanosAsSeconds(lagNano) + " lag)"
                : prettyName + ": " + DungeonTimer.formatSeconds(phaseTime);

        DungeonTimer.lines.add(
                new DungeonTimer.RenderableLine(
                        text,
                        color,
                        optionalItemIcon
                )
        );
    }

    private static final void updateDungeonTimer() {
        // TODO add config option
        if (null == MinecraftClient.getInstance().player || !LocationUtils.isInDungeons()) {
            DungeonTimer.skipRender = true;
            return;
        }

        DungeonTimer.lastClientNow = System.nanoTime();
        DungeonTimer.lastServerTickNow = DungeonTimer.serverTickCounter;

        final var expectedServerNano =
                DungeonTimer.lastServerTickNow * DungeonTimer.TICK_NANOS;

        final var globalLagNow =
                Math.max(0L, DungeonTimer.lastClientNow - expectedServerNano);

        DungeonTimer.lastMonotonicGlobalLagNano =
                Math.max(DungeonTimer.lastMonotonicGlobalLagNano, globalLagNow);

        DungeonTimer.lines.clear();

        DungeonTimer.addLine(DungeonTimer.DungeonPhase.DUNGEON_START, DungeonTimer.DungeonPhase.BLOOD_OPEN, "Blood Open", Formatting.WHITE, Items.SUGAR);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.BLOOD_OPEN, DungeonTimer.DungeonPhase.BLOOD_CLEAR, "Blood Done", Formatting.RED, Items.REDSTONE);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.DUNGEON_START, DungeonTimer.DungeonPhase.BOSS_ENTRY, "Boss Entry", Formatting.DARK_GREEN, Items.END_PORTAL_FRAME);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.BOSS_ENTRY, DungeonTimer.DungeonPhase.PHASE_1_CLEAR, "Maxor", Formatting.AQUA, Items.END_CRYSTAL);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.PHASE_1_CLEAR, DungeonTimer.DungeonPhase.PHASE_2_CLEAR, "Storm", Formatting.DARK_PURPLE, Items.BLAZE_ROD);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.PHASE_2_CLEAR, DungeonTimer.DungeonPhase.TERMINALS_CLEAR, "Terminals", Formatting.YELLOW, Items.COMMAND_BLOCK);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.TERMINALS_CLEAR, DungeonTimer.DungeonPhase.PHASE_3_CLEAR, "Goldor", Formatting.GOLD, Items.GOLDEN_SWORD);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.PHASE_3_CLEAR, DungeonTimer.DungeonPhase.PHASE_4_CLEAR, "Necron", Formatting.DARK_RED, Items.STICK);
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.PHASE_4_CLEAR, DungeonTimer.DungeonPhase.PHASE_5_CLEAR, "Wither King", Formatting.GRAY, Items.WITHER_SKELETON_SKULL); // FIXME this shown in F7 once necron dies
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.BOSS_ENTRY, DungeonTimer.DungeonPhase.PHASE_5_CLEAR, "Boss Total", Formatting.LIGHT_PURPLE, Items.DRAGON_HEAD); // TODO add BOSS_CLEAR phase TODO sometimes is 1s off from the sum of all phases, maybe because BOSS_ENTRY lag lost time is also included in the boss total?
        DungeonTimer.addLine(DungeonTimer.DungeonPhase.DUNGEON_START, DungeonTimer.DungeonPhase.PHASE_5_CLEAR, "Total", Formatting.GREEN, Items.CLOCK); // TODO add DUNGEON_END phase

        DungeonTimer.linesSize = DungeonTimer.lines.size();

        DungeonTimer.skipRender = DungeonTimer.lines.isEmpty();
    }

    private static final void renderDungeonTimer(@NotNull final DrawContext context) {
        if (DungeonTimer.skipRender) {
            return;
        }

        final var client = MinecraftClient.getInstance();

        final var textRenderer = client.textRenderer;
        final var lineHeight = textRenderer.fontHeight;
        final var lineSpacing = 7; // TODO find best value, maybe increase it to 8

        final var iconSize = 16;
        final var iconTextGap = 4;

        final var baseTextX = RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10;
        final var baseIconX = RenderUtils.CHAT_ALIGNED_X;

        final var totalHeight = DungeonTimer.linesSize * (lineHeight + lineSpacing) - lineSpacing;
        final var baseY = RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (totalHeight >> 1);

        for (int i = 0, len = DungeonTimer.linesSize; i < len; ++i) {
            final var line = DungeonTimer.lines.get(i);
            final var y = baseY + i * (lineHeight + lineSpacing);

            final var textX = null == line.optionalItemIcon() ? baseTextX - (iconSize + iconTextGap) : baseTextX;

            if (null != line.optionalItemIcon()) {
                RenderUtils.renderItem(
                        context,
                        line.optionalItemIcon(),
                        baseIconX,
                        y - (iconSize - lineHeight >> 1)
                );
            }

            RenderUtils.renderText(
                    context,
                    line.plaintext(),
                    textX,
                    y,
                    line.color()
            );
        }
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        event.match(DungeonTimer.MESSAGE_HANDLERS);
    }

    private static final void reset(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        DungeonTimer.serverTickCounter = 0L;
        DungeonTimer.lastClientNow = System.nanoTime();
        DungeonTimer.lastServerTickNow = 0L;
        DungeonTimer.DungeonTimingState.resetAll();
    }

    public enum DungeonPhase {
        DUNGEON_START,
        BLOOD_OPEN,
        BLOOD_CLEAR,
        BOSS_ENTRY,
        PHASE_1_CLEAR,
        PHASE_2_CLEAR,
        TERMINALS_CLEAR,
        PHASE_3_CLEAR,
        PHASE_4_CLEAR,
        PHASE_5_CLEAR
    }

    private static final class PhaseTiming {
        private final long clientNano;
        private final long serverTick;

        private final long globalLagNanoAtFinish;

        private long maxLagNano;

        private PhaseTiming(final long clientNano, final long serverTick, final long globalLagNanoAtFinish) {
            super();

            this.clientNano = clientNano;
            this.serverTick = serverTick;
            this.globalLagNanoAtFinish = globalLagNanoAtFinish;
        }

        @NotNull
        private static final DungeonTimer.PhaseTiming now() {
            final var clientNow = System.nanoTime();
            final var serverTickNow = DungeonTimer.serverTickCounter;

            final var expectedServerNano = serverTickNow * DungeonTimer.TICK_NANOS;
            final var lagNano = Math.max(0L, clientNow - expectedServerNano);

            return new DungeonTimer.PhaseTiming(
                    clientNow,
                    serverTickNow,
                    lagNano
            );
        }

        @Override
        public final String toString() {
            return "PhaseTiming{" +
                    "clientNano=" + this.clientNano +
                    ", serverTick=" + this.serverTick +
                    ", globalLagNanoAtFinish=" + this.globalLagNanoAtFinish +
                    ", maxLagNano=" + this.maxLagNano +
                    '}';
        }
    }

    private static final class DungeonTimingState {
        @NotNull
        private static final Map<DungeonTimer.DungeonPhase, DungeonTimer.PhaseTiming> timings =
                new EnumMap<>(DungeonTimer.DungeonPhase.class);

        private static final void finishedPhase(@NotNull final DungeonTimer.DungeonPhase phase) {
            if (DungeonTimer.DungeonTimingState.timings.containsKey(phase)) {
                DarkUtils.warn(DungeonTimer.DungeonTimingState.class, "Phase " + phase.name() + " was finished multiple times");
            } else {
                DungeonTimer.DungeonTimingState.timings.put(phase, DungeonTimer.PhaseTiming.now());
            }
        }

        @Nullable
        private static final DungeonTimer.PhaseTiming getPhase(@NotNull final DungeonTimer.DungeonPhase phase) {
            return DungeonTimer.DungeonTimingState.timings.get(phase);
        }

        private static final void resetAll() {
            DungeonTimer.DungeonTimingState.timings.clear();
        }
    }

    private record RenderableLine(
            String plaintext,
            Formatting color,
            @Nullable Item optionalItemIcon
    ) {
    }
}
