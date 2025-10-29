package gg.darkutils.feat.dungeons;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.InteractEntityEvent;
import gg.darkutils.events.ReceiveMainThreadPacketEvent;
import gg.darkutils.events.RenderWorldEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.TickUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class AlignmentTaskSolver {
    private static final @NotNull BlockPos topLeft = new BlockPos(-2, 124, 79);
    private static final @NotNull BlockPos bottomRight = new BlockPos(-2, 120, 75);

    private static final @NotNull List<BlockPos> box;
    private static final @NotNull ObjectLinkedOpenHashSet<AlignmentTaskSolver.MazeSpace> grid = new ObjectLinkedOpenHashSet<>();
    private static final @NotNull Object2IntOpenHashMap<AlignmentTaskSolver.Point> directionSet = new Object2IntOpenHashMap<>();
    private static final @NotNull Object2IntOpenHashMap<BlockPos> clicks = new Object2IntOpenHashMap<>();
    private static final @NotNull Object2IntOpenHashMap<BlockPos> pendingClicks = new Object2IntOpenHashMap<>();

    private static final @NotNull Direction @NotNull [] directions = AlignmentTaskSolver.getDirections().toArray(new Direction[0]);

    static {
        // Sort the box
        final var temp = new ObjectArrayList<BlockPos>();

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

        box = List.copyOf(temp);

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

    private static final void sanityCheckBoxes(final @NotNull List<BlockPos> boxes) {
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
                final var frames = new ObjectArrayList<ItemFrameEntity>();
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
                final var startPositions = new ObjectArrayList<AlignmentTaskSolver.MazeSpace>();
                final var endPositions = new ObjectArrayList<AlignmentTaskSolver.MazeSpace>();
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

        EventRegistry.centralRegistry().addListener(AlignmentTaskSolver::onRenderWorld);
        EventRegistry.centralRegistry().addListener(AlignmentTaskSolver::onInteractEntity);
        EventRegistry.centralRegistry().addListener(AlignmentTaskSolver::onPacketReceive);
    }

    private static final void onWorldUnload() {
        AlignmentTaskSolver.grid.clear();
        AlignmentTaskSolver.directionSet.clear();
        AlignmentTaskSolver.pendingClicks.clear();
    }

    private static final void onRenderWorld(@NotNull final RenderWorldEvent event) {
        if (!AlignmentTaskSolver.isInPhase3()) {
            return;
        }
        for (final var space : AlignmentTaskSolver.grid) {
            if (AlignmentTaskSolver.SpaceType.PATH != space.type || null == space.framePos) {
                continue;
            }
            final var neededClicks = AlignmentTaskSolver.clicks.getOrDefault(space.framePos, 0);
            if (0 == neededClicks) {
                continue;
            }
            final var pending = AlignmentTaskSolver.pendingClicks.getOrDefault(space.framePos, 0);
            AlignmentTaskSolver.showNametagAtFrame(space.framePos, Integer.toString(0 < pending ? neededClicks - pending : neededClicks), 0 < pending && 0 == neededClicks - pending ? Formatting.GREEN : Formatting.RED);
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

    private static final void onInteractEntity(final @NotNull InteractEntityEvent event) {
        final var e = event.entity();

        if (!AlignmentTaskSolver.directionSet.isEmpty()
                && DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolver
                && AlignmentTaskSolver.isSolverActive()
                && e instanceof final ItemFrameEntity entity) {
            final var pos = entity.getBlockPos();

            final var clickCount = AlignmentTaskSolver.clicks.getInt(pos);
            final var pending = AlignmentTaskSolver.pendingClicks.getOrDefault(pos, 0);

            if (DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolverBlockIncorrectClicks) {
                if (clickCount == pending) {
                    event.cancellationState().cancel();
                } else {
                    if (null != MinecraftClient.getInstance().world) {
                        final var behind = pos.offset(entity.getHorizontalFacing().getOpposite());
                        if (MinecraftClient.getInstance().world.getBlockState(behind).isOf(Blocks.SEA_LANTERN)) {
                            event.cancellationState().cancel();
                        }
                    }
                }
            }

            if (!event.cancellationState().isCancelled()) {
                AlignmentTaskSolver.pendingClicks.put(pos, pending + 1);
            }
        }
    }

    private static final void onPacketReceive(final @NotNull ReceiveMainThreadPacketEvent event) {
        final var p = event.packet();

        if (!AlignmentTaskSolver.directionSet.isEmpty()
                && DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolver
                && AlignmentTaskSolver.isSolverActive()
                && p instanceof final EntityTrackerUpdateS2CPacket packet
                && null != MinecraftClient.getInstance().world
                && MinecraftClient.getInstance().world.getEntityById(packet.id()) instanceof final ItemFrameEntity entity) {
            final var pos = entity.getBlockPos();
            final var pending = AlignmentTaskSolver.pendingClicks.getOrDefault(pos, -1);

            if (-1 == pending) {
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
                        DarkUtils.error(AlignmentTaskSolver.class, "Tracked value has wrong type {}", null == val ? "null" : val.getClass().getName());
                    }
                    break;
                }
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
            final var currentClicks = AlignmentTaskSolver.clicks.getOrDefault(pos, -1);

            if (-1 == currentClicks || turns != currentClicks) {
                AlignmentTaskSolver.clicks.put(pos, turns);
            }
        }
    }

    /* ----------------- Helper methods replacing Kotlin stdlib ----------------- */

    private static final List<AlignmentTaskSolver.GridMove> convertPointMapToMoves(final List<AlignmentTaskSolver.Point> solution) {
        if (solution.isEmpty()) {
            return Collections.emptyList();
        }

        // Reverse copy
        final var reversed = new ObjectArrayList<>(solution).reversed();

        final var moves = new ObjectArrayList<AlignmentTaskSolver.GridMove>();
        for (int i = 0, len = reversed.size(); i < len - 1; ++i) {
            final var current = reversed.get(i);
            final var next = reversed.get(i + 1);
            final var diffX = current.x - next.x;
            final var diffY = current.y - next.y;

            Direction dir = null;
            for (final var direction : AlignmentTaskSolver.directions) {
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
                case UP, DOWN -> 0;
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
        final var directions = Direction.values();
        final var dirs = new ObjectArrayList<Direction>(directions.length);
        for (final var direction : directions) {
            if (0 == direction.getVector().getY()) {
                dirs.add(direction);
            }
        }
        return dirs.reversed();
    }

    private static final @NotNull List<AlignmentTaskSolver.Point> solve(final int[][] grid, final @NotNull AlignmentTaskSolver.Point start, final @NotNull AlignmentTaskSolver.Point end) {
        final var queue = new ObjectArrayFIFOQueue<AlignmentTaskSolver.Point>();
        final var gridCopy = new AlignmentTaskSolver.Point[grid.length][grid[0].length];
        queue.enqueue(start);
        gridCopy[start.y][start.x] = start;

        while (!queue.isEmpty()) {
            final var currPos = queue.dequeue();
            for (final var dir : AlignmentTaskSolver.directions) {
                final var nextPos = AlignmentTaskSolver.move(grid, gridCopy, currPos, dir);
                if (null != nextPos) {
                    queue.enqueue(nextPos);
                    gridCopy[nextPos.y][nextPos.x] = new AlignmentTaskSolver.Point(currPos.x, currPos.y);
                    if (end.x() == nextPos.x && end.y() == nextPos.y) {
                        final var steps = new ObjectArrayList<AlignmentTaskSolver.Point>();
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
        return Collections.emptyList();
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

    private record Point(int x, int y) implements Comparable<AlignmentTaskSolver.Point> {
        @Override
        public final int compareTo(@NotNull final AlignmentTaskSolver.Point o) {
            final var cmp = Integer.compare(this.x, o.x);
            return 0 == cmp ? Integer.compare(this.y, o.y) : cmp;
        }
    }
}
