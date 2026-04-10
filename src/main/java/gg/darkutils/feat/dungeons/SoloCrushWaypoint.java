package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public final class SoloCrushWaypoint {
    @NotNull
    private static final Formatting SOLO_CRUSH_WAYPOINT_COLOR = Formatting.DARK_PURPLE;
    @NotNull
    private static final BlockPos SOLO_CRUSH_WAYPOINT_POS = new BlockPos(100, 186, 68);
    @NotNull
    private static final BooleanSupplier SHOULD_RENDER = TickUtils.queueUpdatingCondition(SoloCrushWaypoint::shouldRender);

    private SoloCrushWaypoint() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        WorldRenderEvents.END_MAIN.register(SoloCrushWaypoint::renderSoloCrushWaypoint);
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.soloCrushWaypoint;
    }

    private static final boolean isInF7OrM7() {
        return DungeonTimer.isOnDungeonFloor(7);
    }

    private static final boolean isP1FinishedWhileP2IsNot() {
        return DungeonTimer.isInBetweenPhases(DungeonTimer.DungeonPhase.PHASE_1_CLEAR, DungeonTimer.DungeonPhase.PHASE_2_CLEAR);
    }

    private static final boolean shouldRender() {
        return SoloCrushWaypoint.isEnabled() && SoloCrushWaypoint.isInF7OrM7() && SoloCrushWaypoint.isP1FinishedWhileP2IsNot();
    }

    private static final void renderSoloCrushWaypoint(@NotNull final WorldRenderContext context) {
        if (!SoloCrushWaypoint.SHOULD_RENDER.getAsBoolean()) {
            return;
        }

        RenderUtils.drawBlockOutline(context, SoloCrushWaypoint.SOLO_CRUSH_WAYPOINT_POS, SoloCrushWaypoint.SOLO_CRUSH_WAYPOINT_COLOR);
    }
}
