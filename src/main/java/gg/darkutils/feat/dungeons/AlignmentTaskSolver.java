package gg.darkutils.feat.dungeons;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class AlignmentTaskSolver {
    private static final @NotNull BlockPos topLeft = new BlockPos(-2, 124, 79);
    private static final @NotNull BlockPos bottomRight = new BlockPos(-2, 120, 75);

    private static final @NotNull List<BlockPos> box;
    private static final @NotNull LinkedHashSet<AlignmentTaskSolver.MazeSpace> grid = new LinkedHashSet<>();
    private static final @NotNull HashMap<AlignmentTaskSolver.Point, Integer> directionSet = new HashMap<>();
    private static final @NotNull HashMap<BlockPos, Integer> clicks = new HashMap<>();
    private static final @NotNull HashMap<BlockPos, Integer> pendingClicks = new HashMap<>();

    static {
        // Sort the box
        final var temp = new ArrayList<BlockPos>();

        for (final var pos : BlockPos.iterate(AlignmentTaskSolver.topLeft, AlignmentTaskSolver.bottomRight)) {
            temp.add(pos.toImmutable());
        }

        temp.sort((first, second) -> {
            if (first.getY() == second.getY()) {
                return second.getZ() - first.getZ();
            }
            if (first.getY() < second.getY()) {
                return 1;
            }
            return first.getY() > second.getY() ? -1 : 0;
        });

        box = Collections.unmodifiableList(temp);

        AlignmentTaskSolver.sanityCheckBoxes(AlignmentTaskSolver.box);

        // Start the tick timer
        TickUtils.queueRepeatingTickTask(() -> {
            AlignmentTaskSolver.computeLayout();
            AlignmentTaskSolver.computeTurns();
        }, 20);
    }

    private AlignmentTaskSolver() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    private static final void sanityCheckBoxes(final @NotNull List<? extends BlockPos> boxes) {
        final var expectedBoxes = List.of(new BlockPos(-2, 124, 79), new BlockPos(-2, 124, 78), new BlockPos(-2, 124, 77), new BlockPos(-2, 124, 76), new BlockPos(-2, 124, 75), new BlockPos(-2, 123, 79), new BlockPos(-2, 123, 78), new BlockPos(-2, 123, 77), new BlockPos(-2, 123, 76), new BlockPos(-2, 123, 75), new BlockPos(-2, 122, 79), new BlockPos(-2, 122, 78), new BlockPos(-2, 122, 77), new BlockPos(-2, 122, 76), new BlockPos(-2, 122, 75), new BlockPos(-2, 121, 79), new BlockPos(-2, 121, 78), new BlockPos(-2, 121, 77), new BlockPos(-2, 121, 76), new BlockPos(-2, 121, 75), new BlockPos(-2, 120, 79), new BlockPos(-2, 120, 78), new BlockPos(-2, 120, 77), new BlockPos(-2, 120, 76), new BlockPos(-2, 120, 75));

        if (boxes.size() != expectedBoxes.size()) {
            throw new IllegalStateException("Box sanity check failed: expected size "
                    + expectedBoxes.size() + ", got " + boxes.size());
        }

        for (int i = 0, len = expectedBoxes.size(); i < len; ++i) {
            final var expected = expectedBoxes.get(i);
            final var actual = boxes.get(i);

            if (!expected.equals(actual)) {
                throw new IllegalStateException("Box sanity check failed at index " + i
                        + ": expected " + expected.toShortString() + ", got " + actual.toShortString());
            }
        }
    }

    private static final boolean isInDungeons() {
        return LocationUtils.isInDungeons();
    }

    private static final boolean isInPhase3() {
        return DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolverPredev || 0L != DungeonTimer.phase2ClearTime;
    }

    private static final boolean isSolverActive() {
        return DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolver
                && AlignmentTaskSolver.isInDungeons()
                && null != MinecraftClient.getInstance().player
                && AlignmentTaskSolver.isInPhase3();
    }

    private static final void computeLayout() {
        if (!AlignmentTaskSolver.isSolverActive()) {
            return;
        }
        if (null == MinecraftClient.getInstance().player) {
            return;
        }
        final var x = MinecraftClient.getInstance().player.getX();
        final var y = MinecraftClient.getInstance().player.getY();
        final var z = MinecraftClient.getInstance().player.getZ();
        if (25.0D * 25.0D >= AlignmentTaskSolver.topLeft.getSquaredDistanceFromCenter(x, y, z)) {
            if (25 > AlignmentTaskSolver.grid.size()) {
                final var frames = new ArrayList<ItemFrameEntity>();
                if (null != MinecraftClient.getInstance().world) {
                    for (final var entity : MinecraftClient.getInstance().world.getEntities()) {
                        if (entity instanceof final ItemFrameEntity frame && AlignmentTaskSolver.box.contains(frame.getBlockPos())) {
                            final var held = frame.getHeldItemStack();
                            if (null != held && (held.isOf(Items.ARROW) || held.isOf(Items.RED_WOOL) || held.isOf(Items.LIME_WOOL))) {
                                frames.add(frame);
                            }
                        }
                    }
                }
                if (!frames.isEmpty()) {
                    for (int i = 0, len = AlignmentTaskSolver.box.size(); i < len; ++i) {
                        final var pos = AlignmentTaskSolver.box.get(i);
                        final var coords = new AlignmentTaskSolver.Point(i % 5, i / 5);

                        ItemFrameEntity frame = null;
                        for (final var itemFrame : frames) {
                            if (pos.equals(itemFrame.getBlockPos())) {
                                frame = itemFrame;
                                break;
                            }
                        }

                        final var type = AlignmentTaskSolver.getSpaceType(frame);
                        AlignmentTaskSolver.grid.add(new AlignmentTaskSolver.MazeSpace(null == frame ? null : frame.getBlockPos(), type, coords));
                    }
                }
            } else if (AlignmentTaskSolver.directionSet.isEmpty()) {
                final var startPositions = new ArrayList<AlignmentTaskSolver.MazeSpace>();
                final var endPositions = new ArrayList<AlignmentTaskSolver.MazeSpace>();
                for (final var space : AlignmentTaskSolver.grid) {
                    if (AlignmentTaskSolver.SpaceType.STARTER == space.type) {
                        startPositions.add(space);
                    }
                    if (AlignmentTaskSolver.SpaceType.END == space.type) {
                        endPositions.add(space);
                    }
                }

                final var layout = AlignmentTaskSolver.getLayout();
                for (final var start : startPositions) {
                    for (final var end : endPositions) {
                        final var pointMap = AlignmentTaskSolver.solve(layout, start.coords, end.coords);
                        final var moves = AlignmentTaskSolver.convertPointMapToMoves(pointMap);
                        for (final var move : moves) {
                            AlignmentTaskSolver.directionSet.put(move.point, move.directionNum);
                        }
                    }
                }
            }
        }
    }

    private static final @NotNull AlignmentTaskSolver.SpaceType getSpaceType(final @Nullable ItemFrameEntity frame) {
        if (null != frame) {
            final var held = frame.getHeldItemStack();
            if (null != held) {
                if (held.isOf(Items.ARROW)) {
                    return AlignmentTaskSolver.SpaceType.PATH;
                }
                if (held.isOf(Items.RED_WOOL)) {
                    return AlignmentTaskSolver.SpaceType.END;
                }
                return held.isOf(Items.LIME_WOOL) ? AlignmentTaskSolver.SpaceType.STARTER : AlignmentTaskSolver.SpaceType.EMPTY;
            }
        }
        return AlignmentTaskSolver.SpaceType.EMPTY;
    }

    private static final void computeTurns() {
        if (null == MinecraftClient.getInstance().world || AlignmentTaskSolver.directionSet.isEmpty()) {
            return;
        }
        for (final var space : AlignmentTaskSolver.grid) {
            if (AlignmentTaskSolver.SpaceType.PATH != space.type || null == space.framePos) {
                continue;
            }
            ItemFrameEntity frame = null;
            for (final var entity : MinecraftClient.getInstance().world.getEntities()) {
                if (entity instanceof final ItemFrameEntity frameEntity && space.framePos.equals(frameEntity.getBlockPos())) {
                    frame = frameEntity;
                    break;
                }
            }
            if (null == frame) {
                continue;
            }
            final var neededClicks = AlignmentTaskSolver.getTurnsNeeded(frame.getRotation(), AlignmentTaskSolver.directionSet.getOrDefault(space.coords, 0));
            AlignmentTaskSolver.clicks.put(space.framePos, neededClicks);
        }
    }

    private static final int getTurnsNeeded(final int current, final int needed) {
        return (needed - current + 8) % 8;
    }

    public static final void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> AlignmentTaskSolver.onWorldUnload());
    }

    private static final void onWorldUnload() {
        AlignmentTaskSolver.grid.clear();
        AlignmentTaskSolver.directionSet.clear();
        AlignmentTaskSolver.pendingClicks.clear();
    }

    public static final void onRenderWorld() {
        if (!AlignmentTaskSolver.isInPhase3()) {
            return;
        }
        for (final var space : AlignmentTaskSolver.grid) {
            if (AlignmentTaskSolver.SpaceType.PATH != space.type || null == space.framePos) {
                continue;
            }
            final var neededClicks = AlignmentTaskSolver.clicks.get(space.framePos);
            if (null == neededClicks || 0 == neededClicks) {
                continue;
            }
            final int pending = AlignmentTaskSolver.pendingClicks.getOrDefault(space.framePos, 0);
            AlignmentTaskSolver.showNametagAtFrame(space.framePos, 0 < pending ? Integer.toString(neededClicks - pending) : Integer.toString(neededClicks), 0 < pending && 0 == neededClicks - pending ? Formatting.GREEN : Formatting.RED);
        }
    }

    private static final void showNametagAtFrame(final @NotNull BlockPos pos, final @NotNull String text, final @NotNull Formatting formatting) {
        final var world = MinecraftClient.getInstance().world;
        if (null == world) {
            return;
        }

        ItemFrameEntity entity = null;
        for (final var e : world.getEntities()) {
            if (e instanceof final ItemFrameEntity frame && pos.equals(frame.getBlockPos())) {
                entity = frame;
                break;
            }
        }

        if (null == entity) {
            return;
        }

        final var name = Text.literal(text).setStyle(Style.EMPTY.withColor(formatting));
        final var stack = entity.getHeldItemStack();

        stack.set(DataComponentTypes.CUSTOM_NAME, name);
    }

    public static final boolean onPacketSend(final @NotNull Entity e) {
        var allowPacket = true;

        if (!AlignmentTaskSolver.directionSet.isEmpty()
                && DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolver
                && AlignmentTaskSolver.isSolverActive()
                && e instanceof final ItemFrameEntity entity) {
            final var pos = entity.getBlockPos();

            final var clickCount = AlignmentTaskSolver.clicks.get(pos);
            final var pending = AlignmentTaskSolver.pendingClicks.getOrDefault(pos, 0);

            if (DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolverBlockIncorrectClicks) {
                if (Objects.equals(clickCount, pending)) {
                    allowPacket = false;
                } else {
                    if (null != MinecraftClient.getInstance().world) {
                        final var behind = pos.offset(entity.getHorizontalFacing().getOpposite());
                        if (MinecraftClient.getInstance().world.getBlockState(behind).isOf(Blocks.SEA_LANTERN)) {
                            allowPacket = false;
                        }
                    }
                }
            }

            if (allowPacket) {
                AlignmentTaskSolver.pendingClicks.put(pos, pending + 1);
            }
        }

        return allowPacket;
    }

    public static final void onPacketReceive(final @NotNull Packet<?> p) {
        if (!AlignmentTaskSolver.directionSet.isEmpty()
                && DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolver
                && AlignmentTaskSolver.isSolverActive()
                && p instanceof final EntityTrackerUpdateS2CPacket packet
                && null != MinecraftClient.getInstance().world
                && MinecraftClient.getInstance().world.getEntityById(packet.id()) instanceof final ItemFrameEntity entity) {
            final var pos = entity.getBlockPos();
            final var pending = AlignmentTaskSolver.pendingClicks.get(pos);

            if (null == pending) {
                return;
            }

            // Extract rotation value (id 10)
            Integer rotationVarInt = null;
            for (final var tracked : packet.trackedValues()) {
                final var id = tracked.id();
                if (10 == id) {
                    final var val = tracked.value();
                    if (val instanceof final Integer i) {
                        rotationVarInt = i;
                    } else {
                        DarkUtils.logError(AlignmentTaskSolver.class, "Tracked value has wrong type {}", null == val ? "null" : val.getClass().getName());
                    }
                    break;
                }
                DarkUtils.logError(AlignmentTaskSolver.class, "Unknown tracked value with id {}", id);
            }

            if (null == rotationVarInt) {
                return;
            }

            final int newRot = rotationVarInt;
            final var currentRot = entity.getRotation();
            final var delta = AlignmentTaskSolver.getTurnsNeeded(currentRot, newRot);
            final var newPending = pending - delta;

            AlignmentTaskSolver.pendingClicks.put(pos, newPending);

            // Sync clicks
            AlignmentTaskSolver.MazeSpace space = null;
            for (final var gridSpace : AlignmentTaskSolver.grid) {
                if (pos.equals(gridSpace.framePos)) {
                    space = gridSpace;
                    break;
                }
            }

            if (null == space) {
                return;
            }

            final var turns = AlignmentTaskSolver.getTurnsNeeded(newRot, AlignmentTaskSolver.directionSet.getOrDefault(space.coords, 0));
            final var currentClicks = AlignmentTaskSolver.clicks.get(pos);

            if (null == currentClicks || turns != currentClicks) {
                AlignmentTaskSolver.clicks.put(pos, turns);
            }
        }
    }

    /* ----------------- Helper methods replacing Kotlin stdlib ----------------- */

    private static final List<AlignmentTaskSolver.GridMove> convertPointMapToMoves(final ArrayList<AlignmentTaskSolver.Point> solution) {
        if (solution.isEmpty()) {
            return Collections.emptyList();
        }

        // Reverse copy
        final var reversed = new ArrayList<>(solution).reversed();

        final var moves = new ArrayList<AlignmentTaskSolver.GridMove>();
        for (int i = 0, len = reversed.size(); i < len - 1; ++i) {
            final var current = reversed.get(i);
            final var next = reversed.get(i + 1);
            final var diffX = current.x - next.x;
            final var diffY = current.y - next.y;

            Direction dir = null;
            for (final var direction : AlignmentTaskSolver.getDirections()) {
                if (direction.getVector().getX() == diffX && direction.getVector().getZ() == diffY) {
                    dir = direction;
                    break;
                }
            }

            if (null == dir) {
                continue;
            }

            final var rotation = switch (dir.getOpposite()) {
                case EAST -> 1;
                case WEST -> 5;
                case SOUTH -> 3;
                case NORTH -> 7;
                default -> 0;
            };

            moves.add(new AlignmentTaskSolver.GridMove(current, rotation));
        }
        return moves;
    }

    private static final int[][] getLayout() {
        final var arr = new int[5][5];
        for (var row = 0; 4 >= row; ++row) {
            for (var col = 0; 4 >= col; ++col) {
                AlignmentTaskSolver.MazeSpace found = null;
                for (final var space : AlignmentTaskSolver.grid) {
                    if (space.coords.x() == row && space.coords.y() == col) {
                        found = space;
                        break;
                    }
                }
                arr[col][row] = null != found && null != found.framePos ? 0 : 1;
            }
        }
        return arr;
    }

    private static final @NotNull List<Direction> getDirections() {
        final var dirs = new ArrayList<Direction>();
        for (final var direction : Direction.values()) {
            if (0 == direction.getVector().getY()) {
                dirs.add(direction);
            }
        }
        return dirs.reversed();
    }

    private static final @NotNull ArrayList<AlignmentTaskSolver.Point> solve(final int[][] grid, final @NotNull AlignmentTaskSolver.Point start, final @NotNull AlignmentTaskSolver.Point end) {
        final var queue = new ArrayDeque<AlignmentTaskSolver.Point>();
        final var gridCopy = new AlignmentTaskSolver.Point[grid.length][grid[0].length];
        queue.addLast(start);
        gridCopy[start.y][start.x] = start;

        final var dirs = AlignmentTaskSolver.getDirections();

        while (!queue.isEmpty()) {
            final var currPos = queue.removeFirst();
            for (final var dir : dirs) {
                final var nextPos = AlignmentTaskSolver.move(grid, gridCopy, currPos, dir);
                if (null != nextPos) {
                    queue.addLast(nextPos);
                    gridCopy[nextPos.y][nextPos.x] = new AlignmentTaskSolver.Point(currPos.x, currPos.y);
                    if (end.x() == nextPos.x && end.y() == nextPos.y) {
                        final var steps = new ArrayList<AlignmentTaskSolver.Point>();
                        steps.add(nextPos);
                        steps.add(currPos);
                        var tmp = currPos;
                        while (!tmp.equals(start)) {
                            tmp = gridCopy[tmp.y][tmp.x];
                            steps.add(tmp);
                        }
                        return steps;
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private static final @Nullable AlignmentTaskSolver.Point move(final int @NotNull [][] grid, final AlignmentTaskSolver.Point[][] gridCopy, final AlignmentTaskSolver.Point currPos, final Direction dir) {
        final var x = currPos.x;
        final var y = currPos.y;
        final var diffX = dir.getVector().getX();
        final var diffY = dir.getVector().getZ();
        final var i = 0 <= x + diffX && x + diffX < grid[0].length
                && 0 <= y + diffY && y + diffY < grid.length
                && 1 != grid[y + diffY][x + diffX] ? 1 : 0;
        return null == gridCopy[y + i * diffY][x + i * diffX] ? new AlignmentTaskSolver.Point(x + i * diffX, y + i * diffY) : null;
    }

    /* ----------------- Inner Types ----------------- */

    private enum SpaceType {
        EMPTY,
        PATH,
        STARTER,
        END
    }

    private record MazeSpace(@Nullable BlockPos framePos, @NotNull AlignmentTaskSolver.SpaceType type,
                             @NotNull AlignmentTaskSolver.Point coords) {
    }

    private record GridMove(@NotNull AlignmentTaskSolver.Point point, int directionNum) {
    }

    private record Point(int x, int y) {
    }
}
