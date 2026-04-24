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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AlignmentTaskSolver {
    private static final @NotNull BlockPos topLeft = new BlockPos(-2, 124, 79);
    private static final @NotNull BlockPos bottomRight = new BlockPos(-2, 120, 75);

    private static final @NotNull List<BlockPos> box;
    private static final @NotNull Set<BlockPos> boxSet;
    private static final @NotNull AABB boxAABB;
    private static final @NotNull ObjectLinkedOpenHashSet<AlignmentTaskSolver.MazeSpace> grid = new ObjectLinkedOpenHashSet<>();
    private static final @NotNull Object2IntOpenHashMap<AlignmentTaskSolver.Point> directionSet = new Object2IntOpenHashMap<>();
    private static final @NotNull Object2IntOpenHashMap<BlockPos> clicks = new Object2IntOpenHashMap<>();
    private static final @NotNull Object2IntOpenHashMap<BlockPos> pendingClicks = new Object2IntOpenHashMap<>();

    private static final @NotNull List<@NotNull Direction> directions = List.copyOf(AlignmentTaskSolver.getDirections());

    static {
        // Sort the box
        final var temp = new ObjectArrayList<BlockPos>();

        for (final var pos : BlockPos.betweenClosed(AlignmentTaskSolver.topLeft, AlignmentTaskSolver.bottomRight)) {
            temp.add(pos.immutable());
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
        boxSet = Set.copyOf(AlignmentTaskSolver.box);

        boxAABB = new AABB(
                Math.min(AlignmentTaskSolver.topLeft.getX(), AlignmentTaskSolver.bottomRight.getX()) - 1.0D,
                Math.min(AlignmentTaskSolver.topLeft.getY(), AlignmentTaskSolver.bottomRight.getY()) - 1.0D,
                Math.min(AlignmentTaskSolver.topLeft.getZ(), AlignmentTaskSolver.bottomRight.getZ()) - 1.0D,

                Math.max(AlignmentTaskSolver.topLeft.getX(), AlignmentTaskSolver.bottomRight.getX()) + 2.0D,
                Math.max(AlignmentTaskSolver.topLeft.getY(), AlignmentTaskSolver.bottomRight.getY()) + 2.0D,
                Math.max(AlignmentTaskSolver.topLeft.getZ(), AlignmentTaskSolver.bottomRight.getZ()) + 2.0D
        );

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
        final var expectedBoxes = new ArrayList<BlockPos>(25);

        for (var y = 124; 120 <= y; --y) {
            for (var z = 79; 75 <= z; --z) {
                expectedBoxes.add(new BlockPos(-2, y, z));
            }
        }

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
        return DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolverPredev || DungeonTimer.isPhaseFinished(DungeonTimer.DungeonPhase.PHASE_2_CLEAR);
    }

    private static final boolean isSolverActive() {
        return DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolver
                && null != Minecraft.getInstance().level
                && null != Minecraft.getInstance().player
                && AlignmentTaskSolver.isInDungeons()
                && AlignmentTaskSolver.isInPhase3();
    }

    private static final void computeLayout() {
        if (!AlignmentTaskSolver.isSolverActive()) {
            return;
        }

        final var player = Minecraft.getInstance().player;
        if (null == player) {
            return;
        }

        final var px = player.getX();
        final var py = player.getY();
        final var pz = player.getZ();

        if (!AlignmentTaskSolver.isWithinTopLeftDistance(px, py, pz)) {
            return;
        }

        if (25 > AlignmentTaskSolver.grid.size()) {
            AlignmentTaskSolver.computeGrid();
        } else if (AlignmentTaskSolver.directionSet.isEmpty()) {
            AlignmentTaskSolver.computeDirections();
        }
    }

    private static final boolean isWithinTopLeftDistance(final double x, final double y, final double z) {
        return 625.0D >= AlignmentTaskSolver.topLeft.distToCenterSqr(x, y, z);
    }

    private static final void computeGrid() {
        final var frames = AlignmentTaskSolver.collectRelevantItemFrames();
        if (frames.isEmpty()) {
            return;
        }

        for (int i = 0, len = AlignmentTaskSolver.box.size(); i < len; ++i) {
            final var pos = AlignmentTaskSolver.box.get(i);
            final var coords = new AlignmentTaskSolver.Point(i % 5, i / 5);
            final var frame = AlignmentTaskSolver.findFrameAtPos(frames, pos);

            final var type = AlignmentTaskSolver.getSpaceType(frame);

            final var framePos = null == frame ? null : frame.blockPosition();
            final var frameBox = null == framePos ? null : new AABB(
                    framePos.getX() - 1.0D,
                    framePos.getY() - 1.0D,
                    framePos.getZ() - 1.0D,

                    framePos.getX() + 2.0D,
                    framePos.getY() + 2.0D,
                    framePos.getZ() + 2.0D
            );

            AlignmentTaskSolver.grid.add(new AlignmentTaskSolver.MazeSpace(framePos, frameBox, type, coords));
        }
    }

    @NotNull
    private static final ObjectArrayList<ItemFrame> collectRelevantItemFrames() {
        final var frames = new ObjectArrayList<ItemFrame>();
        final var world = Minecraft.getInstance().level;
        if (null == world) {
            return frames;
        }

        for (final var frame : world.getEntities(EntityType.ITEM_FRAME, AlignmentTaskSolver.boxAABB, ignored -> true)) {
            if (!AlignmentTaskSolver.boxSet.contains(frame.blockPosition())) {
                continue;
            }

            final var held = frame.getItem();
            if (held.is(Items.ARROW) || held.is(Items.RED_WOOL) || held.is(Items.LIME_WOOL)) {
                frames.add(frame);
            }
        }
        return frames;
    }

    @Nullable
    private static final ItemFrame findFrameAtPos(@NotNull final ObjectArrayList<ItemFrame> frames, @NotNull final BlockPos pos) {
        for (final var frame : frames) {
            if (pos.equals(frame.blockPosition())) {
                return frame;
            }
        }
        return null;
    }

    private static final void computeDirections() {
        final var startPositions = new ObjectArrayList<AlignmentTaskSolver.MazeSpace>();
        final var endPositions = new ObjectArrayList<AlignmentTaskSolver.MazeSpace>();

        for (final var space : AlignmentTaskSolver.grid) {
            switch (space.type()) {
                case STARTER -> startPositions.add(space);
                case END -> endPositions.add(space);
                case EMPTY, PATH -> {
                    // we only care about start and end
                }
                default -> throw new IllegalStateException("Unexpected value: " + space.type());
            }
        }

        final var layout = AlignmentTaskSolver.getLayout();
        for (final var start : startPositions) {
            for (final var end : endPositions) {
                final var pointMap = AlignmentTaskSolver.solve(layout, start.coords(), end.coords());
                final var moves = AlignmentTaskSolver.convertPointMapToMoves(pointMap);
                for (final var move : moves) {
                    AlignmentTaskSolver.directionSet.put(move.point(), move.directionNum());
                }
            }
        }
    }

    private static final @NotNull AlignmentTaskSolver.SpaceType getSpaceType(final @Nullable ItemFrame frame) {
        if (null != frame) {
            final var held = frame.getItem();
            if (held.is(Items.ARROW)) {
                return AlignmentTaskSolver.SpaceType.PATH;
            }
            if (held.is(Items.RED_WOOL)) {
                return AlignmentTaskSolver.SpaceType.END;
            }
            return held.is(Items.LIME_WOOL) ? AlignmentTaskSolver.SpaceType.STARTER : AlignmentTaskSolver.SpaceType.EMPTY;
        }
        return AlignmentTaskSolver.SpaceType.EMPTY;
    }

    private static final void computeTurns() {
        final var world = Minecraft.getInstance().level;
        if (null == world || !AlignmentTaskSolver.isSolverActive() || AlignmentTaskSolver.directionSet.isEmpty()) {
            return;
        }
        for (final var space : AlignmentTaskSolver.grid) {
            final var framePos = space.framePos();
            final var frameBox = space.frameBox();
            if (AlignmentTaskSolver.SpaceType.PATH != space.type() || null == framePos || null == frameBox) {
                continue;
            }
            ItemFrame frame = null;
            for (final var frameEntity : world.getEntities(EntityType.ITEM_FRAME, frameBox, ignored -> true)) {
                if (framePos.equals(frameEntity.blockPosition())) {
                    frame = frameEntity;
                    break;
                }
            }
            if (null == frame) {
                continue;
            }
            final var neededClicks = AlignmentTaskSolver.getTurnsNeeded(frame.getRotation(), AlignmentTaskSolver.directionSet.getOrDefault(space.coords(), 0));
            AlignmentTaskSolver.clicks.put(space.framePos(), neededClicks);
        }
    }

    private static final int getTurnsNeeded(final int current, final int needed) {
        return (needed - current + 8) % 8;
    }

    public static final void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(AlignmentTaskSolver::onWorldUnload);

        EventRegistry.centralRegistry().addListener(AlignmentTaskSolver::onRenderWorld);
        EventRegistry.centralRegistry().addListener(AlignmentTaskSolver::onInteractEntity);
        EventRegistry.centralRegistry().addListener(AlignmentTaskSolver::onPacketReceive);
    }

    private static final void onWorldUnload(@NotNull final Minecraft client, @NotNull final ClientLevel world) {
        AlignmentTaskSolver.grid.clear();
        AlignmentTaskSolver.directionSet.clear();
        AlignmentTaskSolver.pendingClicks.clear();
    }

    private static final void onRenderWorld(@NotNull final RenderWorldEvent event) {
        if (!AlignmentTaskSolver.isSolverActive()) {
            return;
        }
        for (final var space : AlignmentTaskSolver.grid) {
            final var pos = space.framePos();
            final var box = space.frameBox();
            if (AlignmentTaskSolver.SpaceType.PATH != space.type() || null == pos || null == box) {
                continue;
            }
            final var neededClicks = AlignmentTaskSolver.clicks.getOrDefault(pos, 0);
            if (0 == neededClicks) {
                continue;
            }
            final var pending = AlignmentTaskSolver.pendingClicks.getOrDefault(pos, 0);
            AlignmentTaskSolver.showNametagAtFrame(pos, box, Integer.toString(0 < pending ? neededClicks - pending : neededClicks), 0 < pending && 0 == neededClicks - pending ? ChatFormatting.GREEN : ChatFormatting.RED);
        }
    }

    private static final void showNametagAtFrame(final @NotNull BlockPos pos, final @NotNull AABB box, final @NotNull String text, final @NotNull ChatFormatting formatting) {
        final var world = Minecraft.getInstance().level;
        if (null == world) {
            return;
        }

        ItemFrame entity = null;
        for (final var frame : world.getEntities(EntityType.ITEM_FRAME, box, ignored -> true)) {
            if (pos.equals(frame.blockPosition())) {
                entity = frame;
                break;
            }
        }

        if (null == entity) {
            return;
        }

        final var name = Component.literal(text).setStyle(Style.EMPTY.withColor(formatting));
        final var stack = entity.getItem();

        stack.set(DataComponents.CUSTOM_NAME, name);
    }

    private static final void onInteractEntity(final @NotNull InteractEntityEvent event) {
        final var e = event.entity();

        if (AlignmentTaskSolver.isSolverActive()
                && !AlignmentTaskSolver.directionSet.isEmpty()
                && e instanceof final ItemFrame entity) {
            final var pos = entity.blockPosition();

            final var clickCount = AlignmentTaskSolver.clicks.getInt(pos);
            final var pending = AlignmentTaskSolver.pendingClicks.getOrDefault(pos, 0);

            if (DarkUtilsConfig.INSTANCE.arrowAlignmentDeviceSolverBlockIncorrectClicks) {
                if (clickCount == pending) {
                    event.cancellationState().cancel();
                } else {
                    final var world = Minecraft.getInstance().level;
                    if (null != world) {
                        final var behind = pos.relative(entity.getDirection().getOpposite());
                        if (world.getBlockState(behind).is(Blocks.SEA_LANTERN)) {
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

    private static final void onPacketReceive(@NotNull final ReceiveMainThreadPacketEvent event) {
        if (!AlignmentTaskSolver.isSolverActive() || AlignmentTaskSolver.directionSet.isEmpty() || !(event.packet() instanceof final ClientboundSetEntityDataPacket packet)) {
            return;
        }

        final var entity = AlignmentTaskSolver.getItemFrameEntity(packet);
        if (null == entity) {
            return;
        }

        final var pos = entity.blockPosition();
        final var pending = AlignmentTaskSolver.pendingClicks.getOrDefault(pos, -1);
        if (-1 == pending) {
            return;
        }

        final var rotationVarInt = AlignmentTaskSolver.extractRotation(packet);
        if (null == rotationVarInt) {
            return;
        }

        AlignmentTaskSolver.updatePendingClicks(entity, pos, pending, rotationVarInt);
        AlignmentTaskSolver.syncClicks(pos, rotationVarInt);
    }

    private static final @Nullable ItemFrame getItemFrameEntity(@NotNull final ClientboundSetEntityDataPacket packet) {
        final var world = Minecraft.getInstance().level;
        if (null == world) {
            return null;
        }
        final var entity = world.getEntity(packet.id());
        return entity instanceof final ItemFrame frame ? frame : null;
    }

    private static final @Nullable Integer extractRotation(@NotNull final ClientboundSetEntityDataPacket packet) {
        for (final var tracked : packet.packedItems()) {
            if (10 == tracked.id()) {
                final var val = tracked.value();
                if (val instanceof final Integer i) {
                    return i;
                }
                DarkUtils.error(AlignmentTaskSolver.class, "Tracked value has wrong type {}", null == val ? "null" : val.getClass().getName());
                return null;
            }
        }
        return null;
    }

    private static final void updatePendingClicks(@NotNull final ItemFrame entity, @NotNull final BlockPos pos, final int pending, final int newRot) {
        final var currentRot = entity.getRotation();
        final var delta = AlignmentTaskSolver.getTurnsNeeded(currentRot, newRot);
        final var newPending = pending - delta;
        AlignmentTaskSolver.pendingClicks.put(pos, newPending);
    }

    private static final void syncClicks(@NotNull final BlockPos pos, final int newRot) {
        final var space = AlignmentTaskSolver.findGridSpace(pos);
        if (null == space) {
            return;
        }

        final var turns = AlignmentTaskSolver.getTurnsNeeded(newRot, AlignmentTaskSolver.directionSet.getOrDefault(space.coords(), 0));
        final var currentClicks = AlignmentTaskSolver.clicks.getOrDefault(pos, -1);

        if (-1 == currentClicks || turns != currentClicks) {
            AlignmentTaskSolver.clicks.put(pos, turns);
        }
    }

    private static final @Nullable AlignmentTaskSolver.MazeSpace findGridSpace(@NotNull final BlockPos pos) {
        for (final var gridSpace : AlignmentTaskSolver.grid) {
            final var framePos = gridSpace.framePos();
            if (null != framePos && pos.equals(framePos)) {
                return gridSpace;
            }
        }
        return null;
    }

    private static final @NonNull List<AlignmentTaskSolver.GridMove> convertPointMapToMoves(final List<AlignmentTaskSolver.Point> solution) {
        if (solution.isEmpty()) {
            return List.of();
        }

        // Reverse copy
        final var reversed = new ObjectArrayList<>(solution).reversed();

        final var moves = new ObjectArrayList<AlignmentTaskSolver.GridMove>();
        for (int i = 0, len = reversed.size(); i < len - 1; ++i) {
            final var current = reversed.get(i);
            final var next = reversed.get(i + 1);
            final var diffX = current.x() - next.x();
            final var diffY = current.y() - next.y();

            Direction dir = null;
            for (final var direction : AlignmentTaskSolver.directions) {
                if (direction.getUnitVec3i().getX() == diffX && direction.getUnitVec3i().getZ() == diffY) {
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
        return List.copyOf(moves);
    }

    private static final int[][] getLayout() {
        final var arr = new int[5][5];
        for (var row = 0; 4 >= row; ++row) {
            for (var col = 0; 4 >= col; ++col) {
                AlignmentTaskSolver.MazeSpace found = null;
                for (final var space : AlignmentTaskSolver.grid) {
                    if (space.coords().x() == row && space.coords().y() == col) {
                        found = space;
                        break;
                    }
                }
                arr[col][row] = null != found && null != found.framePos() ? 0 : 1;
            }
        }
        return arr;
    }

    private static final @NotNull List<Direction> getDirections() {
        final var directions = Direction.values();
        final var dirs = new ObjectArrayList<Direction>(directions.length);
        for (final var direction : directions) {
            if (0 == direction.getUnitVec3i().getY()) {
                dirs.add(direction);
            }
        }
        return dirs.reversed();
    }

    private static final @NotNull List<AlignmentTaskSolver.Point> solve(final int[][] grid, final @NotNull AlignmentTaskSolver.Point start, final @NotNull AlignmentTaskSolver.Point end) {
        final var queue = new ObjectArrayFIFOQueue<AlignmentTaskSolver.Point>();
        final var gridCopy = new AlignmentTaskSolver.Point[grid.length][grid[0].length];
        queue.enqueue(start);
        gridCopy[start.y()][start.x()] = start;

        while (!queue.isEmpty()) {
            final var currPos = queue.dequeue();
            for (final var dir : AlignmentTaskSolver.directions) {
                final var nextPos = AlignmentTaskSolver.move(grid, gridCopy, currPos, dir);
                if (null != nextPos) {
                    queue.enqueue(nextPos);
                    gridCopy[nextPos.y()][nextPos.x()] = new AlignmentTaskSolver.Point(currPos.x(), currPos.y());
                    if (end.x() == nextPos.x() && end.y() == nextPos.y()) {
                        final var steps = new ObjectArrayList<AlignmentTaskSolver.Point>();
                        steps.add(nextPos);
                        steps.add(currPos);
                        for (var tmp = currPos; !tmp.equals(start); ) {
                            tmp = gridCopy[tmp.y()][tmp.x()];
                            steps.add(tmp);
                        }
                        return List.copyOf(steps);
                    }
                }
            }
        }
        return List.of();
    }

    private static final @Nullable AlignmentTaskSolver.Point move(final int @NotNull [][] grid, final AlignmentTaskSolver.Point[][] gridCopy, final AlignmentTaskSolver.Point currPos, final Direction dir) {
        final var x = currPos.x();
        final var y = currPos.y();
        final var diffX = dir.getUnitVec3i().getX();
        final var diffY = dir.getUnitVec3i().getZ();
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

    private record MazeSpace(@Nullable BlockPos framePos,
                             @Nullable AABB frameBox,
                             @NotNull AlignmentTaskSolver.SpaceType type,
                             @NotNull AlignmentTaskSolver.Point coords) {
    }

    private record GridMove(@NotNull AlignmentTaskSolver.Point point, int directionNum) {
    }

    private record Point(int x, int y) implements Comparable<AlignmentTaskSolver.Point> {
        @Override
        public final int compareTo(@NotNull final AlignmentTaskSolver.Point o) {
            final var cmp = Integer.compare(this.x(), o.x());
            return 0 == cmp ? Integer.compare(this.y(), o.y()) : cmp;
        }
    }
}
