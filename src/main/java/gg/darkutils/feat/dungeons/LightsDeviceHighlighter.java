package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import org.jetbrains.annotations.NotNull;

public final class LightsDeviceHighlighter {
    // The six lever positions used for Skyblocker's optimal finish.
    private static final @NotNull BlockPos[] LEVERS = {
            new BlockPos(62, 136, 142), new BlockPos(58, 136, 142),
            new BlockPos(60, 135, 142), new BlockPos(60, 134, 142),
            new BlockPos(62, 133, 142), new BlockPos(58, 133, 142)
    };
    private static final @NotNull float[] RED = RenderUtils.getFloatComponents(DyeColor.RED.getTextureDiffuseColor());
    private static final float ALPHA = 0.75F;

    private LightsDeviceHighlighter() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        LevelRenderEvents.BEFORE_GIZMOS.register(LightsDeviceHighlighter::render);
    }

    private static final void render(@NotNull final LevelRenderContext context) {
        if (!DarkUtilsConfig.INSTANCE.highlightLightsDeviceOptimalFinish || !LocationUtils.isInDungeons()
                || !DungeonTimer.isOnDungeonFloor(7)
                || !DungeonTimer.isInBetweenPhases(DungeonTimer.DungeonPhase.BOSS_ENTRY, DungeonTimer.DungeonPhase.TERMINALS_CLEAR)) {
            return;
        }

        final var level = Minecraft.getInstance().level;
        if (null == level) {
            return;
        }

        for (final var pos : LightsDeviceHighlighter.LEVERS) {
            final var state = level.getBlockState(pos);
            if (!state.is(Blocks.LEVER) || state.getValue(LeverBlock.POWERED)) {
                continue;
            }

            final var box = RenderUtils.getBlockBoundingBox(level, state, pos);
            if (null != box) {
                RenderUtils.drawFilledBox(box, LightsDeviceHighlighter.RED, LightsDeviceHighlighter.ALPHA);
            }
        }
    }
}
