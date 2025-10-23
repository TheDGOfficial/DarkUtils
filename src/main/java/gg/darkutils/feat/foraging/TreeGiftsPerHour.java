package gg.darkutils.feat.foraging;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.TreeGiftObtainedEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.MathUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.RoundingMode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class TreeGiftsPerHour {
    private static final int SAMPLE_SIZE = 5;
    private static final long @NotNull [] giftTimes = new long[TreeGiftsPerHour.SAMPLE_SIZE];
    private static int giftCount; // how many valid entries
    private static int giftIndex; // next index to write
    private static long lastGiftTime;

    private TreeGiftsPerHour() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    private static final void reset() {
        TreeGiftsPerHour.lastGiftTime = 0L;
        TreeGiftsPerHour.giftCount = 0;
        TreeGiftsPerHour.giftIndex = 0;
    }

    private static final void addGiftDuration(final long durationNanos) {
        TreeGiftsPerHour.giftTimes[TreeGiftsPerHour.giftIndex] = durationNanos;
        TreeGiftsPerHour.giftIndex = (TreeGiftsPerHour.giftIndex + 1) % TreeGiftsPerHour.SAMPLE_SIZE;
        if (TreeGiftsPerHour.SAMPLE_SIZE > TreeGiftsPerHour.giftCount) {
            ++TreeGiftsPerHour.giftCount;
        }
    }

    private static final double getAvgGiftTimeMillis() {
        if (0 == TreeGiftsPerHour.giftCount) {
            return 0.0;
        }
        var sumNanos = 0L;
        for (var i = 0; TreeGiftsPerHour.giftCount > i; ++i) {
            sumNanos += TreeGiftsPerHour.giftTimes[(TreeGiftsPerHour.giftIndex + TreeGiftsPerHour.SAMPLE_SIZE - TreeGiftsPerHour.giftCount + i) % TreeGiftsPerHour.SAMPLE_SIZE];
        }
        final var avgNanos = (double) sumNanos / TreeGiftsPerHour.giftCount;
        return avgNanos / TimeUnit.MILLISECONDS.toNanos(1L);
    }

    private static final double getGiftsPerHour() {
        final var avgMillis = TreeGiftsPerHour.getAvgGiftTimeMillis();
        return 0.0 >= avgMillis ? 0.0 : TimeUnit.HOURS.toMillis(1L) / avgMillis;
    }

    public static final void init() {
        EventRegistry.centralRegistry().<TreeGiftObtainedEvent>addListener(event -> TreeGiftsPerHour.onTreeGift());
        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "tree_gifts_per_hour"), (context, tickCounter) -> TreeGiftsPerHour.renderTreeGifts(context));
    }

    private static final void renderTreeGifts(@NotNull final DrawContext context) {
        if (!DarkUtilsConfig.INSTANCE.treeGiftsPerHour) {
            // Prevent leaking samples if feature is turned off after using it
            TreeGiftsPerHour.reset();
            return;
        }

        // ⏱️ Skip if last gift was more than 1 minute ago
        if (0L != TreeGiftsPerHour.lastGiftTime) {
            final var now = System.nanoTime();
            final var oneMinuteNanos = TimeUnit.MINUTES.toNanos(1L);
            if (now - TreeGiftsPerHour.lastGiftTime > oneMinuteNanos) {
                return;
            }
        }

        final var client = MinecraftClient.getInstance();

        if (null == client.player) {
            return;
        }

        final var perHour = TreeGiftsPerHour.getGiftsPerHour();

        if (0.0 >= perHour) {
            return;
        }

        final var text = "Tree Gifts/Hour: " + MathUtils.round(perHour, RoundingMode.HALF_DOWN);

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y,
                Formatting.DARK_GREEN
        );
    }

    private static final void onTreeGift() {
        if (!DarkUtilsConfig.INSTANCE.treeGiftsPerHour) {
            // Prevent leaking samples if feature is turned off after using it
            TreeGiftsPerHour.reset();
            return;
        }

        final var now = System.nanoTime();

        if (0L != TreeGiftsPerHour.lastGiftTime) {
            final var durationNanos = now - TreeGiftsPerHour.lastGiftTime;
            TreeGiftsPerHour.addGiftDuration(durationNanos);
        }

        // Update for next interval
        TreeGiftsPerHour.lastGiftTime = now;
    }
}
