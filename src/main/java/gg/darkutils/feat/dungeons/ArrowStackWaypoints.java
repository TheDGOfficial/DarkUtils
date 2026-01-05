package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BooleanSupplier;

public final class ArrowStackWaypoints {
    /**
     * Holds block positions for M7 Dragon Last Breath arrow stacking.
     */
    @NotNull
    private static final Map<Formatting, BlockPos> STACK_BLOCK_POSITIONS = Map.of(
            Formatting.GOLD, new BlockPos(83, 19, 57), // Orange/Flame Dragon
            Formatting.GREEN, new BlockPos(26, 21, 92), // Green/Apex Dragon
            Formatting.DARK_RED, new BlockPos(27, 19, 56), // Red/Power Dragon
            Formatting.AQUA, new BlockPos(82, 19, 96), // Blue/Ice Dragon
            Formatting.DARK_PURPLE, new BlockPos(56, 20, 124) // Purple/Soul Dragon
    );

    @NotNull
    private static final BooleanSupplier SHOULD_RENDER =
            TickUtils.queueUpdatingCondition(ArrowStackWaypoints::shouldRender);

    private ArrowStackWaypoints() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        WorldRenderEvents.END_MAIN.register(ArrowStackWaypoints::renderArrowStackWaypoints);
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.arrowStackWaypoints;
    }

    private static final boolean shouldRender() {
        final ClientPlayerEntity player;
        return ArrowStackWaypoints.isEnabled() && (0L != DungeonTimer.phase4ClearTime || null != (player = MinecraftClient.getInstance().player) && 45.0D >= player.getY()) && 0L == DungeonTimer.phase5ClearTime && 0L != DungeonTimer.bossEntryTime/* && LocationUtils.isInM7()*/;
    }

    private static final void renderArrowStackWaypoints(@NotNull final WorldRenderContext context) {
        if (!ArrowStackWaypoints.SHOULD_RENDER.getAsBoolean()) {
            return;
        }

        ArrowStackWaypoints.STACK_BLOCK_POSITIONS.forEach((color, pos) -> RenderUtils.drawBlockOutline(context, pos, color));
    }
}
