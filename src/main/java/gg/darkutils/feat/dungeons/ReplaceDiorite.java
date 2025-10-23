package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public final class ReplaceDiorite {
    private static final BlockState @NotNull [] glassStates = new BlockState[16];
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
    private static final Long2ObjectOpenHashMap<ObjectOpenHashSet<BlockPos>> chunkToPositions =
            new Long2ObjectOpenHashMap<>(4);

    static {
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

            ReplaceDiorite.glassStates[color.ordinal()] = state;
        }
    }

    private ReplaceDiorite() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ReplaceDiorite.addPillar(new BlockPos(46, 169, 41), 5);
        ReplaceDiorite.addPillar(new BlockPos(46, 169, 65), 4);
        ReplaceDiorite.addPillar(new BlockPos(100, 169, 65), 10);
        ReplaceDiorite.addPillar(new BlockPos(100, 169, 41), 14);

        // register tick callback
        ClientTickEvents.END_CLIENT_TICK.register(ReplaceDiorite::onTick);
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
                    ReplaceDiorite.chunkToPositions.computeIfAbsent(key, l -> new ObjectOpenHashSet<>(1_862)).add(pos);
                }
            }
        }
    }

    private static final void onTick(@NotNull final MinecraftClient client) {
        final var world = client.world;

        if (null == world) {
            return;
        }

        if (DarkUtilsConfig.INSTANCE.replaceDiorite
                && 0L != DungeonTimer.bossEntryTime
                && 0L == DungeonTimer.phase2ClearTime) {
            ReplaceDiorite.replaceDiorite(world);
        }
    }

    private static final void replaceDiorite(@NotNull final ClientWorld world) {
        for (final var entry : ReplaceDiorite.chunkToPositions.long2ObjectEntrySet()) {
            final var key = entry.getLongKey();
            final var chunkX = (int) (key >> 32);
            final var chunkZ = (int) (key & 0xFFFF_FFFFL);

            final var chunk = world.getChunk(chunkX, chunkZ);

            if (null == chunk) {
                continue; // skip unloaded
            }

            for (final var pos : entry.getValue()) {
                final var state = chunk.getBlockState(pos);

                if (state.isOf(Blocks.DIORITE) || state.isOf(Blocks.POLISHED_DIORITE)
                        || state.isOf(Blocks.GRANITE) || state.isOf(Blocks.POLISHED_GRANITE)
                        || state.isOf(Blocks.ANDESITE) || state.isOf(Blocks.POLISHED_ANDESITE)) {
                    ReplaceDiorite.setGlass(world, pos);
                }
            }
        }
    }

    private static final void setGlass(@NotNull final ClientWorld world, @NotNull final BlockPos pos) {
        final var color = ReplaceDiorite.posToColor.getOrDefault(pos, -1);
        if (-1 != color) {
            world.setBlockState(pos, ReplaceDiorite.glassStates[color], 3);
        }
    }
}
