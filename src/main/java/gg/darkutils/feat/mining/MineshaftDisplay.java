package gg.darkutils.feat.mining;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.PrettyUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TabListUtil;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class MineshaftDisplay {
    private static final long ONE_SECOND_IN_NS = TimeUnit.SECONDS.toNanos(1L);
    private static final long TEN_SECOND_IN_NS = TimeUnit.SECONDS.toNanos(10L);
    private static final long ONE_MINUTE_IN_NS = TimeUnit.MINUTES.toNanos(1L);

    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    @NotNull
    private static final RenderUtils.RenderingText PITY_TEXT =
            RenderUtils.createRenderingText();

    private static int mineShaftPity = -1;
    private static int mineShaftPityRequired = -1; // normally always 2000, but just in case it changes in future

    private static boolean shownWarpClose;

    private MineshaftDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(MineshaftDisplay::onWorldChange);

        TickUtils.queueRepeatingTickTask(MineshaftDisplay::updateTabData, 60); // tab updates every 3s server-side anyway, no need to update more frequently

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "mineshaft_display"), (context, tickCounter) -> MineshaftDisplay.renderMineshaftDisplay(context));
    }

    private static final void onWorldChange(@NotNull final Minecraft client, @NotNull final ClientLevel world) {
        MineshaftDisplay.shownWarpClose = false;
    }

    private static final void updateTabData() {
        if (!LocationUtils.isInDwarvenMines()) {
            return;
        }

        MineshaftDisplay.mineShaftPity = -1;
        MineshaftDisplay.mineShaftPityRequired = -1;

        for (final var line : TabListUtil.tabListLines()) {
            if (line.startsWith(" Glacite Mineshafts: ")) {
                final var cleaned = line.replace(" Glacite Mineshafts: ", "");
                final var split = cleaned.split("/");

                final var pity = split[0];
                final int parsedPity;

                try {
                    parsedPity = Integer.parseInt(pity.replace(",", ""));
                } catch (final NumberFormatException nfe) {
                    DarkUtils.error(MineshaftDisplay.class, "Error parsing Mineshaft pity \"" + pity + "\" from tablist widget", nfe);
                    return;
                }

                final var pityRequired = split[1];
                final int parsedPityRequired;

                try {
                    parsedPityRequired = Integer.parseInt(pityRequired.replace(",", "")); // doesn't have commas but just in case
                } catch (final NumberFormatException nfe) {
                    DarkUtils.error(MineshaftDisplay.class, "Error parsing Mineshaft pity required \"" + pityRequired + "\" from tablist widget", nfe);
                    return;
                }

                MineshaftDisplay.mineShaftPity = parsedPity;
                MineshaftDisplay.mineShaftPityRequired = parsedPityRequired;

                break;
            }
        }
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.mineshaftDisplay;
    }

    private static final void displayWarpCloseTimer(final long shaftEntered, final long shaftUptime, final long warpRemaining) {
        if (0L != shaftEntered && shaftUptime < MineshaftDisplay.ONE_MINUTE_IN_NS && warpRemaining <= MineshaftDisplay.TEN_SECOND_IN_NS && !MineshaftDisplay.shownWarpClose) {
            MineshaftDisplay.shownWarpClose = true;
            Helpers.displayCountdownTitles("§6", "Warping closed", 10, LocationUtils::isInMineshaft);
        }
    }

    private static final @NotNull String buildDisplayText() {
        final var now = System.nanoTime();

        final var shaftEntered = MineshaftFeatures.mineshaftEnter;
        final var shaftUptime = now - shaftEntered;

        final var timeSinceLastShaftSpawn = PrettyUtils.prettifyNanosToSeconds(MineshaftFeatures.activeMiningTimeSinceLastShaftSpawn);

        final var averageSpawnTime = PrettyUtils.prettifyNanosToSeconds((long) MineshaftFeatures.averageSpawnTime);
        final var averageTimeInShaft = PrettyUtils.prettifyNanosToSeconds((long) MineshaftFeatures.averageTimeInShaft);

        final var hasUnenteredMineshaft = 0L != MineshaftFeatures.mineshaftDespawnsAt;

        final var remainingTimeToDespawnNs = MineshaftFeatures.mineshaftDespawnsAt - now;

        final var warpRemaining = MineshaftDisplay.ONE_MINUTE_IN_NS - shaftUptime;

        final var omitLast = hasUnenteredMineshaft || "0s".equals(timeSinceLastShaftSpawn);
        final var omitAvgSpawn = "0s".equals(averageSpawnTime);

        final var t = "Mineshaft: " +
                (omitLast ? "" : "Mining for " + timeSinceLastShaftSpawn + (ActivityState.isActivelyMining() ? "" : " [PAUSED]")) +
                (omitAvgSpawn ? "" : (omitLast ? "Avg " : ", Avg ") + averageSpawnTime + " to spawn") +
                ("0s".equals(averageTimeInShaft) ? "" : (omitAvgSpawn && omitLast ? "Avg " : ", Avg ") + averageTimeInShaft + " to loot") +
                (0L == shaftEntered ? hasUnenteredMineshaft ? " - Time till despawn: " + (remainingTimeToDespawnNs >= MineshaftDisplay.ONE_SECOND_IN_NS ? PrettyUtils.prettifyNanosToSeconds(remainingTimeToDespawnNs) : "Despawned") : "" : shaftUptime >= MineshaftDisplay.ONE_MINUTE_IN_NS ? " - In shaft for: " + PrettyUtils.prettifyNanosToSeconds(shaftUptime) : " - Time till warp closure: " + PrettyUtils.prettifyNanosToSeconds(warpRemaining));

        MineshaftDisplay.displayWarpCloseTimer(shaftEntered, shaftUptime, warpRemaining);

        return "Mineshaft: ".equals(t.trim()) ? "No shafts yet" : t;
    }

    private static final void renderMineshaftDisplay(@NotNull final GuiGraphics context) {
        if (!MineshaftDisplay.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();
        final var mineshaft = LocationUtils.isInMineshaft();

        if (null == client.player || !LocationUtils.isInDwarvenMines() && !mineshaft) {
            return;
        }

        final var text = MineshaftDisplay.TEXT;

        text.setText(MineshaftDisplay.buildDisplayText());

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
                ChatFormatting.AQUA
        );

        if (!mineshaft) {
            MineshaftDisplay.renderPity(context);
        }
    }

    private static final void renderPity(@NotNull final GuiGraphics context) {
        final var pity = MineshaftDisplay.mineShaftPity;
        final var required = MineshaftDisplay.mineShaftPityRequired;

        final var pityText = MineshaftDisplay.PITY_TEXT;
        final var known = -1 != pity && -1 != required;

        pityText.setText(known ? "Mineshaft Pity: " + pity + '/' + required : "Please enable the pity widget!");

        // offset a bit so that it shows under mineshaft display
        final var Y_OFFSET = 20;

        RenderUtils.renderItem(
                context,
                Items.GOLD_INGOT,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) + Y_OFFSET // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
        );

        RenderUtils.renderText(
                context,
                pityText,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() + Y_OFFSET,
                known ? ChatFormatting.DARK_GREEN : ChatFormatting.RED
        );
    }
}

