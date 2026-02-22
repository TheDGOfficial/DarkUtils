package gg.darkutils.feat.dungeons;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.SimpleColor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final long FIVE_SECOND_NANOS = TimeUnit.SECONDS.toNanos(5L);
    @NotNull
    private static final ArrayList<RenderableLine> PHASE_LINES =
            new ArrayList<>(16);
    @NotNull
    private static final ArrayList<RenderableLine> activeLines =
            new ArrayList<>(16);
    private static long serverTickCounter;
    @NotNull
    private static final Consumer<ReceiveGameMessageEvent> PHASE_5_FINISH = event -> {
        if (event.isStyledWith(SimpleColor.DARK_RED)) {
            DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_5_CLEAR);
        }
    };
    private static long lastClientNow;
    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.ofEntries(
            Map.entry("Starting in 1 second.", event -> {
                if (event.isStyledWith(SimpleColor.GREEN)) {
                    TickUtils.queueServerTickTask(() -> {
                        DungeonTimer.resetLagModel();
                        DungeonTimer.DungeonTimingState.resetAll();
                        DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.DUNGEON_START);
                    }, 20);
                }
            }),
            Map.entry("The BLOOD DOOR has been opened!", event -> {
                if (event.isStyledWith(SimpleColor.RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.BLOOD_OPEN);
                }
            }),
            Map.entry("[BOSS] The Watcher: You have proven yourself. You may pass.", event -> {
                if (event.isStyledWith(SimpleColor.RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.BLOOD_CLEAR);

                    if (DarkUtilsConfig.INSTANCE.bloodClearedNotification) {
                        final var seconds = DungeonTimer.getPhaseTimeInSecondsForPhase(DungeonTimer.DungeonPhase.BLOOD_OPEN, DungeonTimer.DungeonPhase.BLOOD_CLEAR, false);
                        Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, "§cBlood Cleared! (" + seconds + "s)", 60);
                    }
                }
            }),
            Map.entry("[BOSS] Sadan: ENOUGH!", event -> {
                if (event.isStyledWith(SimpleColor.RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.TERRAS_CLEAR);
                }
            }),
            Map.entry("[BOSS] Sadan: You did it. I understand now, you have earned my respect.", event -> {
                if (event.isStyledWith(SimpleColor.RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.GIANTS_CLEAR);
                }
            }),
            Map.entry("[BOSS] Storm: Pathetic Maxor, just like expected.", event -> {
                if (event.isStyledWith(SimpleColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_1_CLEAR);
                }
            }),
            Map.entry("[BOSS] Goldor: Who dares trespass into my domain?", event -> {
                if (event.isStyledWith(SimpleColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_2_CLEAR);
                }
            }),
            Map.entry("The Core entrance is opening!", event -> {
                if (event.isStyledWith(SimpleColor.GREEN)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.TERMINALS_CLEAR);
                }
            }),
            Map.entry("[BOSS] Necron: You went further than any human before, congratulations.", event -> {
                if (event.isStyledWith(SimpleColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_3_CLEAR);
                }
            }),
            Map.entry("[BOSS] Necron: All this, for nothing...", event -> {
                if (event.isStyledWith(SimpleColor.DARK_RED)) {
                    DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.PHASE_4_CLEAR);
                }
            }),
            Map.entry("[BOSS] Wither King: Incredible. You did what I couldn't do myself.", DungeonTimer.PHASE_5_FINISH),
            Map.entry("[BOSS] Wither King: Thank you for coming all the way here.", DungeonTimer.PHASE_5_FINISH)
    );
    private static long lastServerTickNow;
    private static long lastMonotonicGlobalLagNano;
    private static int linesSize;
    private static int slotIndex;
    private static boolean skipRender = true;
    @Nullable
    private static DungeonFloor dungeonFloor;
    private static boolean warningLogged;

    private DungeonTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final boolean isPhaseFinished(@NotNull final DungeonTimer.DungeonPhase phase) {
        return !DungeonTimer.isPhaseNotFinished(phase);
    }

    public static final boolean isPhaseNotFinished(@NotNull final DungeonTimer.DungeonPhase phase) {
        return null == DungeonTimer.DungeonTimingState.getPhase(phase);
    }

    public static final boolean isInBetweenPhases(@NotNull final DungeonTimer.DungeonPhase started, @NotNull final DungeonTimer.DungeonPhase notYetFinished) {
        if (notYetFinished.ordinal() <= started.ordinal()) {
            throw new IllegalArgumentException(
                    "notYetFinished (" + notYetFinished + ") must come after started (" + started + ')'
            );
        }

        if (null != started.floor && null != notYetFinished.floor && started.floor.floor() != notYetFinished.floor.floor()) {
            throw new IllegalArgumentException(
                    "Incompatible phase pair: "
                            + started + " <-> " + notYetFinished
            );
        }

        return DungeonTimer.isPhaseFinished(started) && DungeonTimer.isPhaseNotFinished(notYetFinished);
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(DungeonTimer::onChat);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(DungeonTimer::reset);

        TickUtils.queueRepeatingServerTickTask(DungeonTimer::onServerTick, 1);

        TickUtils.queueRepeatingTickTask(DungeonTimer::updateDungeonFloor, 1);
        TickUtils.queueRepeatingTickTask(DungeonTimer::updateDungeonTimer, 1);
        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "dungeon_timer"), (context, tickCounter) -> DungeonTimer.renderDungeonTimer(context));
    }

    private static final void onServerTick() {
        if (!LocationUtils.isInDungeons()) {
            return;
        }

        ++DungeonTimer.serverTickCounter;
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

    private static final int nextRenderSlot() {
        return DungeonTimer.slotIndex++;
    }

    private static final void line(@NotNull final DungeonTimer.DungeonPhase start, @NotNull final DungeonTimer.DungeonPhase end, @NotNull final String prettyName, @NotNull final Formatting color, @Nullable final Item optionalItemIcon) {
        final int slot = DungeonTimer.nextRenderSlot();

        final RenderableLine line;

        if (slot >= DungeonTimer.PHASE_LINES.size()) {
            line = new RenderableLine(RenderUtils.createRenderingText(), color, optionalItemIcon);
            DungeonTimer.PHASE_LINES.add(line);
        } else {
            final var existing = DungeonTimer.PHASE_LINES.get(slot);

            if (existing.optionalItemIcon() != optionalItemIcon || existing.color() != color) {
                line = new RenderableLine(
                    existing.renderingText(),
                    color,
                    optionalItemIcon
                );

                DungeonTimer.PHASE_LINES.set(slot, line);
            } else {
                line = existing;
            }
        }

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

        line.renderingText().setText(text);
        DungeonTimer.activeLines.add(line);
    }

    private static long lastNegativeLagWarnNano;

    private static final void handleNegativeLag(
            final long rawLagNano,
            final long serverTick,
            final long clientNow
    ) {
        final long now = System.nanoTime();

        if (DungeonTimer.FIVE_SECOND_NANOS >
                now - DungeonTimer.lastNegativeLagWarnNano) {
            return;
        }

        DungeonTimer.lastNegativeLagWarnNano = now;

        final long aheadMillis =
                TimeUnit.NANOSECONDS.toMillis(-rawLagNano);

        DarkUtils.warn(
                DungeonTimer.class,
                "Server tick model predicted FUTURE time! " +
                "ahead=" + aheadMillis + "ms " +
                "serverTick=" + serverTick
        );

        if (DungeonTimer.TICK_NANOS < -rawLagNano) {
            DarkUtils.warn(
                    DungeonTimer.class,
                    "Server predicted >1 tick into future!"
            );
        }
    }

    private static final void updateDungeonTimer() {
        if (!DarkUtilsConfig.INSTANCE.dungeonTimer || null == MinecraftClient.getInstance().player || null == MinecraftClient.getInstance().world || !LocationUtils.isInDungeons()) {
            DungeonTimer.skipRender = true;
            return;
        }

        DungeonTimer.lastClientNow = System.nanoTime();
        DungeonTimer.lastServerTickNow = DungeonTimer.serverTickCounter;

        final var expectedServerNano =
                DungeonTimer.lastServerTickNow * DungeonTimer.TICK_NANOS;

        final var rawLagNano =
                DungeonTimer.lastClientNow - expectedServerNano;

        if (0L > rawLagNano) {
            DungeonTimer.handleNegativeLag(
                    rawLagNano,
                    DungeonTimer.lastServerTickNow,
                    DungeonTimer.lastClientNow
            );
        }

        final var globalLagNow =
                Math.max(0L, rawLagNano);

        DungeonTimer.lastMonotonicGlobalLagNano =
                Math.max(DungeonTimer.lastMonotonicGlobalLagNano, globalLagNow);

        DungeonTimer.activeLines.clear();

        DungeonTimer.slotIndex = 0;
        DungeonTimer.updateLines();

        DungeonTimer.linesSize = DungeonTimer.activeLines.size();
        DungeonTimer.skipRender = DungeonTimer.activeLines.isEmpty();
    }

    /**
     * Checks that the user is on given dungeon floor.
     * <p>
     * This an exact comparision. See {@link DungeonTimer#isOnDungeonFloor(int)} instead for more relaxed comparision.
     * <p>
     * If you pass a regular floor and user is in a master floor, it won't match.
     * Vice-versa is also true, of course, if you pass a master floor and user is on regular, it won't return true.
     */
    public static final boolean isOnDungeonFloor(@NotNull final DungeonTimer.DungeonFloor dungeonFloor) {
        return dungeonFloor == DungeonTimer.dungeonFloor; // enum-to-enum reference equality is safe. We don't need to call .equals or check that .floor() and .isMaster() is equal on both.
    }

    /**
     * Checks that the user is on given dungeon floor.
     * <p>
     * This will match both master and regular floors.
     */
    public static final boolean isOnDungeonFloor(final int dungeonFloor) {
        final var floor = DungeonTimer.dungeonFloor;
        return null != floor && dungeonFloor == floor.floor();
    }

    /**
     * Gets the dungeon floor the player is on.
     * <p>
     * Returned value is nullable. Regular and master floors are different enum variants.
     * <p>
     * Prefer the methods {@link DungeonTimer#isOnDungeonFloor(DungeonFloor)} or {@link DungeonTimer#isOnDungeonFloor(int)}
     * unless you need the actual enum.
     */
    @Nullable
    public static final DungeonFloor getDungeonFloor() {
        return DungeonTimer.dungeonFloor;
    }

    private static final void warnFloorDetectionIssue(@NotNull final String message) {
        if (DungeonTimer.warningLogged) {
            return;
        }

        DarkUtils.warn(DungeonTimer.class, message);
        DungeonTimer.warningLogged = true;
    }

    public enum DungeonFloor {
        ENTRANCE,
        FLOOR_I,
        FLOOR_II,
        FLOOR_III,
        FLOOR_IV,
        FLOOR_V,
        FLOOR_VI,
        FLOOR_VII,
        MASTER_FLOOR_I,
        MASTER_FLOOR_II,
        MASTER_FLOOR_III,
        MASTER_FLOOR_IV,
        MASTER_FLOOR_V,
        MASTER_FLOOR_VI,
        MASTER_FLOOR_VII;

        private static final DungeonFloor @NotNull [] VALUES = DungeonFloor.values();

        private final boolean master = this.name().startsWith("MASTER");
        private final int floor = master ? this.ordinal() - 7 : this.ordinal();

        @Nullable
        public static final DungeonFloor from(final boolean masterModeFloor, final int rawFloorNumber) {
            final int index = masterModeFloor ? rawFloorNumber + 7 : rawFloorNumber;

            if (0 > index || DungeonFloor.VALUES.length <= index) {
                DungeonTimer.warnFloorDetectionIssue("Unsupported or unknown dungeon floor: " + (masterModeFloor ? 'M' : 'F') + rawFloorNumber);
                return null;
            }

            return DungeonFloor.VALUES[index];
        }

        public final int floor() {
            return this.floor;
        }

        public final boolean isMaster() {
            return this.master;
        }
    }

    @Nullable
    private static final DungeonFloor parseFloorFromScoreboard(@NotNull final String line) {
        final var dungeonFloor = StringUtils.substringBetween(line, "(", ")");

        if (null == dungeonFloor || dungeonFloor.isEmpty()) {
            return null;
        }

        if ("E".equals(dungeonFloor)) {
            return DungeonTimer.DungeonFloor.ENTRANCE;
        }

        if (dungeonFloor.length() < 2) {
            return null;
        }

        final char mode = dungeonFloor.charAt(0);

        if ('M' != mode && 'F' != mode) {
            DungeonTimer.warnFloorDetectionIssue("Unknown dungeon floor prefix: " + dungeonFloor + " (extracted from scoreboard line: " + line + ')');
            return null;
        }

        final var floorPendingParse = dungeonFloor.substring(1);
        final int floor;

        try {
            floor = Integer.parseInt(floorPendingParse);
        } catch (final NumberFormatException nfe) {
            DungeonTimer.warnFloorDetectionIssue("Unexpected floor number " + floorPendingParse + " (extracted from scoreboard line: " + line + ')');
            return null;
        }

        final var isMaster = 'M' == mode;

        return DungeonTimer.DungeonFloor.from(isMaster, floor);
    }

    private static final void updateDungeonFloor() {
        final var mc = MinecraftClient.getInstance();

        if (null == mc.player || null == mc.world || !LocationUtils.isInDungeons() || (null != DungeonTimer.dungeonFloor && DungeonTimer.isPhaseFinished(DungeonTimer.DungeonPhase.DUNGEON_START))) { // If you join an F7 and then join M7 with the command without leaving the F7, the world change event triggers while the scoreboard still says F7, and so you will be in a bugged state in M7 with the floor being detected as F7. To fix this rare bug, we keep re-assigning the dungeon floor till the dungeon starts in addition to the null check.
            return;
        }

        // TODO Extract to a ScoreboardUtil class later
        final var scoreboard = mc.player.networkHandler.getScoreboard();
        final var objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (null == objective) {
            return;
        }

        for (final var scoreHolder : scoreboard.getKnownScoreHolders()) {
            final var team = scoreboard.getScoreHolderTeam(scoreHolder.getNameForScoreboard());

            if (null == team) {
                continue;
            }

            final var line = Formatting.strip(team.getPrefix().getString() + team.getSuffix().getString());

            if (line.contains("The Catacombs (")) {
                final var floor = DungeonTimer.parseFloorFromScoreboard(line);

                if (null != floor) {
                    DungeonTimer.dungeonFloor = floor;
                }

                return;
            }
        }
    }

    private static final void updateLines() {
        final var floor = DungeonTimer.dungeonFloor;

        if (null == floor) {
            return;
        }

        DungeonTimer.line(DungeonTimer.DungeonPhase.DUNGEON_START, DungeonTimer.DungeonPhase.BLOOD_OPEN, "Blood Open", Formatting.WHITE, Items.SUGAR);
        DungeonTimer.line(DungeonTimer.DungeonPhase.BLOOD_OPEN, DungeonTimer.DungeonPhase.BLOOD_CLEAR, "Blood Done", Formatting.RED, Items.REDSTONE);
        DungeonTimer.line(DungeonTimer.DungeonPhase.DUNGEON_START, DungeonTimer.DungeonPhase.BOSS_ENTRY, "Boss Entry", Formatting.DARK_GREEN, Items.END_PORTAL_FRAME);

        // Floor 6
        if (floor.floor() == 6) {
            DungeonTimer.line(DungeonTimer.DungeonPhase.BOSS_ENTRY, DungeonTimer.DungeonPhase.TERRAS_CLEAR, "Terracottas", Formatting.GOLD, Items.BROWN_TERRACOTTA);
            DungeonTimer.line(DungeonTimer.DungeonPhase.TERRAS_CLEAR, DungeonTimer.DungeonPhase.GIANTS_CLEAR, "Giants", Formatting.AQUA, Items.DIAMOND_SWORD);
        }

        // Floor 7 & Master Floor 7
        if (floor.floor() == 7) {
            // Both regular and master 7 have those phases
            DungeonTimer.line(DungeonTimer.DungeonPhase.BOSS_ENTRY, DungeonTimer.DungeonPhase.PHASE_1_CLEAR, "Maxor", Formatting.AQUA, Items.END_CRYSTAL);
            DungeonTimer.line(DungeonTimer.DungeonPhase.PHASE_1_CLEAR, DungeonTimer.DungeonPhase.PHASE_2_CLEAR, "Storm", Formatting.DARK_PURPLE, Items.BLAZE_ROD);
            DungeonTimer.line(DungeonTimer.DungeonPhase.PHASE_2_CLEAR, DungeonTimer.DungeonPhase.TERMINALS_CLEAR, "Terminals", Formatting.YELLOW, Items.COMMAND_BLOCK);
            DungeonTimer.line(DungeonTimer.DungeonPhase.TERMINALS_CLEAR, DungeonTimer.DungeonPhase.PHASE_3_CLEAR, "Goldor", Formatting.GOLD, Items.GOLDEN_SWORD);
            DungeonTimer.line(DungeonTimer.DungeonPhase.PHASE_3_CLEAR, DungeonTimer.DungeonPhase.PHASE_4_CLEAR, "Necron", Formatting.DARK_RED, Items.STICK);

            // Master Floor 7 specific phase
            if (floor.isMaster()) {
                DungeonTimer.line(DungeonTimer.DungeonPhase.PHASE_4_CLEAR, DungeonTimer.DungeonPhase.PHASE_5_CLEAR, "Wither King", Formatting.GRAY, Items.WITHER_SKELETON_SKULL);
            }
        }

        DungeonTimer.line(DungeonTimer.DungeonPhase.BOSS_ENTRY, DungeonTimer.DungeonPhase.BOSS_CLEAR, "Boss Total", Formatting.LIGHT_PURPLE, Items.DRAGON_HEAD); // TODO sometimes is 1s off from the sum of all phases, maybe because BOSS_ENTRY lag lost time is also included in the boss total?
        DungeonTimer.line(DungeonTimer.DungeonPhase.DUNGEON_START, DungeonTimer.DungeonPhase.BOSS_CLEAR, "Total", Formatting.GREEN, Items.CLOCK); // TODO add DUNGEON_END phase for clarity (semantically same as BOSS_CLEAR, as thats where dungeon ends)
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
            final var line = DungeonTimer.activeLines.get(i);
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
                    line.renderingText(),
                    textX,
                    y,
                    line.color()
            );
        }
    }

    private static final void finishAllOpenApplicablePhases() {
        final var timings = DungeonTimingState.timings;
        final var now = PhaseTiming.now();
        final var runFloor = DungeonTimer.dungeonFloor;
        final var values = DungeonTimer.DungeonPhase.values();

        for (var i = 0; i < timings.length; ++i) {
            if (null != timings[i]) {
                continue;
            }

            final var phase = values[i];

            if (!phase.appliesTo(runFloor)) {
                continue;
            }

            timings[i] = now;
        }
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!LocationUtils.isInDungeons()) {
            return;
        }

        event.match(DungeonTimer.MESSAGE_HANDLERS);

        final var noLeadingWhitespace = event.content().stripLeading(); // target matches has leading spaces to center the text added by hypixel

        if ("> EXTRA STATS <".equals(noLeadingWhitespace)) {
            // Sent after Defeated, but failed runs do not send the Defeated one, while they send the Extra Stats text, so we need this in addition.
            if (event.isStyledWith(SimpleColor.GOLD) && DungeonTimer.isPhaseNotFinished(DungeonTimer.DungeonPhase.BOSS_CLEAR)) {
                DungeonTimer.finishAllOpenApplicablePhases(); // implicitly finishes BOSS_CLEAR
            }
        } else if (noLeadingWhitespace.startsWith("☠ Defeated ")) {
            if (event.isStyledWith(SimpleColor.RED)) {
                DungeonTimer.finishAllOpenApplicablePhases(); // implicitly finishes BOSS_CLEAR
            }
        } else {
            final var bossName = event.extractPart("[BOSS] ", ':');

            if (null != bossName && DungeonTimer.ensureCorrectBossForTheFloor(DungeonTimer.dungeonFloor, bossName) && DungeonTimer.isPhaseNotFinished(DungeonTimer.DungeonPhase.BOSS_ENTRY)) {
                DungeonTimer.DungeonTimingState.finishedPhase(DungeonTimer.DungeonPhase.BOSS_ENTRY);
            }
        }
    }

    /**
     * This is necessary to avoid detecting boss messages from The Watcher, Bonzo, Scarf or Livid (in their blood room forms).
     * Only Maxor is checked for Floor 7 as this is currently only used for boss entry detection on first boss dialogue.
     */
    private static final boolean ensureCorrectBossForTheFloor(@Nullable DungeonTimer.DungeonFloor floor, @NotNull final String bossName) {
        if (null == floor) {
            return false;
        }

        final var floorNumber = floor.floor();

        final var correctBoss = switch (floorNumber) {
            case 0 -> "The Watcher";
            case 1 -> "Bonzo";
            case 2 -> "Scarf";
            case 3 -> "The Professor";
            case 4 -> "Thorn";
            case 5 -> "Livid";
            case 6 -> "Sadan";
            case 7 -> "Maxor";
            default -> null;
        };

        return null != correctBoss && correctBoss.equals(bossName);
    }

    private static final void resetLagModel() {
        DungeonTimer.serverTickCounter = 0L;
        DungeonTimer.lastServerTickNow = 0L;
        DungeonTimer.lastMonotonicGlobalLagNano = 0L;
        DungeonTimer.lastClientNow = System.nanoTime();
    }

    private static final void reset(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        DungeonTimer.resetLagModel();

        DungeonTimer.dungeonFloor = null;
        DungeonTimer.activeLines.clear();
        DungeonTimer.PHASE_LINES.clear();
        DungeonTimer.linesSize = 0;
        DungeonTimer.slotIndex = 0;
        DungeonTimer.skipRender = true;
        DungeonTimer.warningLogged = false;

        DungeonTimer.DungeonTimingState.resetAll();
    }

    public enum DungeonPhase {
        // Floor-agnostic
        DUNGEON_START(),
        BLOOD_OPEN(),
        BLOOD_CLEAR(),
        BOSS_ENTRY(),

        // Floor 6
        TERRAS_CLEAR(DungeonFloor.FLOOR_VI),
        GIANTS_CLEAR(DungeonFloor.FLOOR_VI),

        // Floor 7
        PHASE_1_CLEAR(DungeonFloor.FLOOR_VII),
        PHASE_2_CLEAR(DungeonFloor.FLOOR_VII),
        TERMINALS_CLEAR(DungeonFloor.FLOOR_VII),
        PHASE_3_CLEAR(DungeonFloor.FLOOR_VII),
        PHASE_4_CLEAR(DungeonFloor.FLOOR_VII),

        // Master Floor 7
        PHASE_5_CLEAR(DungeonFloor.MASTER_FLOOR_VII),

        // Floor-agnostic
        BOSS_CLEAR();

        @Nullable
        private final DungeonFloor floor;

        private DungeonPhase() {
            this.floor = null;
        }

        private DungeonPhase(@NotNull final DungeonFloor floor) {
            this.floor = floor;
        }

        public final boolean appliesTo(@Nullable final DungeonFloor runFloor) {
            if (null == this.floor) {
                return true;
            }

            if (null == runFloor) {
                return false;
            }

            return this.floor.floor() == runFloor.floor();
        }
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
            final var rawLagNano = clientNow - expectedServerNano;

            if (0L > rawLagNano) {
                DungeonTimer.handleNegativeLag(
                        rawLagNano,
                        serverTickNow,
                        clientNow
                );
            }

            final var lagNano = Math.max(0L, rawLagNano);

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
        private static final DungeonTimer.PhaseTiming @NotNull [] timings = new DungeonTimer.PhaseTiming[DungeonTimer.DungeonPhase.values().length];

        private DungeonTimingState() {
            super();

            throw new UnsupportedOperationException("static class");
        }

        private static final void finishedPhase(@NotNull final DungeonTimer.DungeonPhase phase) {
            final var timings = DungeonTimer.DungeonTimingState.timings;
            final var ordinal = phase.ordinal();

            if (null != timings[ordinal]) {
                DarkUtils.warn(DungeonTimer.DungeonTimingState.class, "Phase " + phase.name() + " was finished multiple times");
                return;
            }

            final var runFloor = DungeonTimer.dungeonFloor;
            final var values = DungeonTimer.DungeonPhase.values();

            for (var i = 0; i < ordinal; ++i) {
                final var earlier = values[i];

                if (!earlier.appliesTo(runFloor)) {
                    continue;
                }

                if (null == timings[i]) {
                    DarkUtils.warn(
                            DungeonTimer.DungeonTimingState.class,
                            "Phase ordering anomaly: "
                                    + phase.name()
                                    + " finished before "
                                    + earlier.name()
                    );
                }
            }

            timings[ordinal] = DungeonTimer.PhaseTiming.now();
        }

        @Nullable
        private static final DungeonTimer.PhaseTiming getPhase(@NotNull final DungeonTimer.DungeonPhase phase) {
            return DungeonTimer.DungeonTimingState.timings[phase.ordinal()];
        }

        private static final void resetAll() {
            Arrays.fill(DungeonTimer.DungeonTimingState.timings, null);
        }
    }

    private record RenderableLine(
            RenderUtils.RenderingText renderingText,
            Formatting color,
            @Nullable Item optionalItemIcon
    ) {
    }
}
