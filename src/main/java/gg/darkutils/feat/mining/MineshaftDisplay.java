package gg.darkutils.feat.mining;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.PrettyUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class MineshaftDisplay {
    private static final long ONE_SECOND_IN_NS = TimeUnit.SECONDS.toNanos(1L);
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
        return nanos < MineshaftDisplay.ONE_MINUTE_IN_NS ? TimeUnit.NANOSECONDS.toSeconds(nanos) + "s" : PrettyUtils.formatNanosAsSeconds(nanos);
    }

    private static final void renderMineshaftDisplay(@NotNull final DrawContext context) {
        if (!MineshaftDisplay.isEnabled()) {
            return;
        }

        final var client = MinecraftClient.getInstance();

        if (null == client.player || !LocationUtils.isInDwarvenMines() && !LocationUtils.isInMineshaft()) {
            return;
        }

        final var shaftEntered = MineshaftFeatures.mineshaftEnter;
        final var shaftUptime = System.nanoTime() - shaftEntered;

        final var timeSinceLastShaft = MineshaftDisplay.prettifyNanosToSeconds(MineshaftFeatures.activeMiningTimeSinceLastShaft);

        final var averageSpawnTime = MineshaftDisplay.prettifyNanosToSeconds((long) MineshaftFeatures.averageSpawnTime);

        final var hasUnenteredMineshaft = 0L != MineshaftFeatures.mineshaftDespawnsAt;

        final var remainingTimeToDespawnNs = MineshaftFeatures.mineshaftDespawnsAt - System.nanoTime();
        final var remainingTimeToDespawn = remainingTimeToDespawnNs >= MineshaftDisplay.ONE_SECOND_IN_NS ? MineshaftDisplay.prettifyNanosToSeconds(remainingTimeToDespawnNs) : "Despawned";

        final var omitLast = hasUnenteredMineshaft || "0s".equals(timeSinceLastShaft);

        final var text = MineshaftDisplay.TEXT;

        text.setText("Mineshaft: " +
                (omitLast ? "" : "Last " + timeSinceLastShaft + " ago" + (ActivityState.isActivelyMining() ? "" : " [PAUSED]")) +
                ("0s".equals(averageSpawnTime) ? "" : (omitLast ? "Avg " : ", Avg ") + averageSpawnTime + " to spawn") +
                (0L == shaftEntered ? hasUnenteredMineshaft ? " - Time till despawn: " + remainingTimeToDespawn : "" : shaftUptime >= MineshaftDisplay.ONE_MINUTE_IN_NS ? " - In shaft for: " + MineshaftDisplay.prettifyNanosToSeconds(shaftUptime) : " - Time till warp closure: " + MineshaftDisplay.prettifyNanosToSeconds(MineshaftDisplay.ONE_MINUTE_IN_NS - shaftUptime))
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

