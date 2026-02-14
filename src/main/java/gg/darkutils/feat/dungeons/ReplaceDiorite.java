package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.EmptyChunk;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReplaceDiorite {
    @NotNull
    private static final List<@NotNull BlockState> glassStates = List.of(ReplaceDiorite.getGlassStates());
    /**
     * 7448 total block positions.
     */
    @NotNull
    private static final Object2IntOpenHashMap<BlockPos> posToColor =
            new Object2IntOpenHashMap<>(7_448);
    /**
     * 4 chunks total.
     */
    @NotNull
    private static final Long2ObjectOpenHashMap<List<BlockPos>> chunkToPositions =
            new Long2ObjectOpenHashMap<>(4);

    private ReplaceDiorite() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    private static final BlockState[] getGlassStates() {
        final var glassStates = new BlockState[16];

        for (final var color : DyeColor.values()) {
            final var state = (switch (color) {
                case WHITE -> Blocks.WHITE_STAINED_GLASS;
                case ORANGE -> Blocks.ORANGE_STAINED_GLASS;
                case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS;
                case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS;
                case YELLOW -> Blocks.YELLOW_STAINED_GLASS;
                case LIME -> Blocks.LIME_STAINED_GLASS;
                case PINK -> Blocks.PINK_STAINED_GLASS;
                case GRAY -> Blocks.GRAY_STAINED_GLASS;
                case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS;
                case CYAN -> Blocks.CYAN_STAINED_GLASS;
                case PURPLE -> Blocks.PURPLE_STAINED_GLASS;
                case BLUE -> Blocks.BLUE_STAINED_GLASS;
                case BROWN -> Blocks.BROWN_STAINED_GLASS;
                case GREEN -> Blocks.GREEN_STAINED_GLASS;
                case RED -> Blocks.RED_STAINED_GLASS;
                case BLACK -> Blocks.BLACK_STAINED_GLASS;
            }).getDefaultState();

            glassStates[color.ordinal()] = state;
        }

        return glassStates;
    }

    public static final void init() {
        ReplaceDiorite.addPillars();

        // register tick callback
        ClientTickEvents.END_CLIENT_TICK.register(ReplaceDiorite::onTick);
    }

    private static final void addPillars() {
        ReplaceDiorite.addPillar(new BlockPos(46, 169, 41), 5);
        ReplaceDiorite.addPillar(new BlockPos(46, 169, 65), 4);
        ReplaceDiorite.addPillar(new BlockPos(100, 169, 65), 10);
        ReplaceDiorite.addPillar(new BlockPos(100, 169, 41), 14);

        for (final var entry : ReplaceDiorite.chunkToPositions.long2ObjectEntrySet()) {
            entry.setValue(List.copyOf(entry.getValue()));
        }
    }

    private static final void addPillar(@NotNull final BlockPos origin, final int color) {
        final var pillarX = origin.getX();
        final var pillarY = origin.getY();
        final var pillarZ = origin.getZ();

        for (var x = pillarX - 3; x <= pillarX + 3; ++x) {
            for (var y = pillarY; y <= pillarY + 37; ++y) {
                for (var z = pillarZ - 3; z <= pillarZ + 3; ++z) {
                    final var pos = new BlockPos(x, y, z);
                    ReplaceDiorite.posToColor.put(pos, color);

                    final var key = (long) (x >> 4) << 32 | (long) z >> 4 & 0xFFFF_FFFFL;
                    ReplaceDiorite.chunkToPositions.computeIfAbsent(key, l -> new ObjectArrayList<>(1_862)).add(pos);
                }
            }
        }
    }

    private static final void onTick(@NotNull final MinecraftClient client) {
        final var world = client.world;

        if (null == world) {
            return;
        }

        if (DarkUtilsConfig.INSTANCE.replaceDiorite && LocationUtils.isInDungeons() && DungeonTimer.isInBetweenPhases(DungeonTimer.DungeonPhase.BOSS_ENTRY, DungeonTimer.DungeonPhase.PHASE_2_CLEAR)) {
            ReplaceDiorite.replaceDiorite(world);
        }
    }

    private static final void replaceDiorite(@NotNull final ClientWorld world) {
        for (final var entry : ReplaceDiorite.chunkToPositions.long2ObjectEntrySet()) {
            final var key = entry.getLongKey();

            final var chunk = world.getChunkAsView((int) (key >> 32), (int) (key & 0xFFFF_FFFFL));

            if (null == chunk || chunk instanceof EmptyChunk) {
                continue; // skip unloaded
            }

            final var positions = entry.getValue();

            // Iterate manually to avoid creating an iterator for less allocation overhead
            for (int i = 0, len = positions.size(); i < len; ++i) {
                final var pos = positions.get(i);

                ReplaceDiorite.setGlassIfDiorite(world, chunk, pos);
            }
        }
    }

    private static final void setGlassIfDiorite(@NotNull final ClientWorld world, @NotNull final BlockView view, @NotNull final BlockPos pos) {
        final var state = view.getBlockState(pos);

        if (state.isOf(Blocks.DIORITE) || state.isOf(Blocks.POLISHED_DIORITE)) {
            ReplaceDiorite.setGlass(world, pos);
        }
    }

    private static final void setGlass(@NotNull final ClientWorld world, @NotNull final BlockPos pos) {
        final var color = ReplaceDiorite.posToColor.getOrDefault(pos, -1);
        if (-1 != color) {
            world.setBlockState(pos, ReplaceDiorite.glassStates.get(color), 3);
        }
    }
}
