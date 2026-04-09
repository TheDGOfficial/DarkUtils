package gg.darkutils.feat.mining;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.data.PersistentData;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.PrettyUtils;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.ActivityState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class MineshaftDisplay {
    private static final long ONE_MINUTE_IN_NS = TimeUnit.MINUTES.toNanos(1L);

    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    private MineshaftDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "mineshaft_display"), (context, tickCounter) -> MineshaftDisplay.renderMineshaftDisplay(context));
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.mineshaftDisplay;
    }

    private static final @NotNull String prettifyNanosToSeconds(final long nanos) {
        if (nanos < ONE_MINUTE_IN_NS) {
            return TimeUnit.NANOSECONDS.toSeconds(nanos) + "s";
        }

        return PrettyUtils.formatNanosAsSeconds(nanos);
    }

    private static final void renderMineshaftDisplay(@NotNull final DrawContext context) {
        if (!MineshaftDisplay.isEnabled()) {
            return;
        }

        final var client = MinecraftClient.getInstance();

        if (null == client.player || (!LocationUtils.isInDwarvenMines() && !LocationUtils.isInMineshaft())) {
            return;
        }

        final var shaftEntered = MineshaftFeatures.mineshaftEnter;
        final var inShaft = 0L != shaftEntered;

        final var shaftUptime = System.nanoTime() - shaftEntered;
        final var shaftClosedForWarping = shaftUptime >= MineshaftDisplay.ONE_MINUTE_IN_NS;
        final var remainingTimeBeforeClosedForWarping = MineshaftDisplay.prettifyNanosToSeconds(MineshaftDisplay.ONE_MINUTE_IN_NS - shaftUptime);

        final var timeSinceLastShaft = MineshaftDisplay.prettifyNanosToSeconds(MineshaftFeatures.activeMiningTimeSinceLastShaft);

        final var averageSpawnTime = MineshaftDisplay.prettifyNanosToSeconds((long) MineshaftFeatures.averageSpawnTime);

        final var hasUnenteredMineshaft = 0L != MineshaftFeatures.mineshaftDespawnsAt;
        final var remainingTimeToDespawn = MineshaftDisplay.prettifyNanosToSeconds(MineshaftFeatures.mineshaftDespawnsAt - System.nanoTime());

        final var text = MineshaftDisplay.TEXT;

        final var omitLast = hasUnenteredMineshaft || "0s".equals(timeSinceLastShaft);
        final var paused = !ActivityState.isActivelyMining();

        text.setText("Mineshaft: " + 
            (omitLast ? "" : "Last " + timeSinceLastShaft + " ago" + (paused ? " [PAUSED]" : "")) +
            ("0s".equals(averageSpawnTime) ? "" : omitLast ? "Avg " + averageSpawnTime + " to spawn" : ", Avg " + averageSpawnTime + " to spawn") +
            (inShaft ? shaftClosedForWarping ? " - In shaft for: " + MineshaftDisplay.prettifyNanosToSeconds(shaftUptime) : " - Time till warp closure: " + remainingTimeBeforeClosedForWarping : hasUnenteredMineshaft ? " - Time till despawn: " + remainingTimeToDespawn : "")
        );

        RenderUtils.renderItem(
                context,
                Items.MINECART,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y,
                Formatting.AQUA
        );
    }
}

