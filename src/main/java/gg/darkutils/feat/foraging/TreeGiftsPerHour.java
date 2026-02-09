package gg.darkutils.feat.foraging;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ObtainTreeGiftEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.MathUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.RoundingMode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final class TreeGiftsPerHour {
    private static final int SAMPLE_SIZE = 5;
    private static final long @NotNull [] giftTimes = new long[TreeGiftsPerHour.SAMPLE_SIZE];
    private static final long ONE_MINUTE_NANOS = TimeUnit.MINUTES.toNanos(1L);
    private static final long ONE_HOUR_MILLIS = TimeUnit.HOURS.toMillis(1L);
    private static final long ONE_MILLIS_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);
    private static int giftCount; // how many valid entries
    private static int giftIndex; // next index to write
    private static long lastGiftTime;

    private TreeGiftsPerHour() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    private static final void reset() {
        Arrays.fill(TreeGiftsPerHour.giftTimes, 0L);
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
        return avgNanos / TreeGiftsPerHour.ONE_MILLIS_NANOS;
    }

    private static final double getGiftsPerHour() {
        final var avgMillis = TreeGiftsPerHour.getAvgGiftTimeMillis();
        return 0.0 >= avgMillis ? 0.0 : TreeGiftsPerHour.ONE_HOUR_MILLIS / avgMillis;
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(TreeGiftsPerHour::onTreeGift);
        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "tree_gifts_per_hour"), (context, tickCounter) -> TreeGiftsPerHour.renderTreeGifts(context));
    }

    private static final void renderTreeGifts(@NotNull final DrawContext context) {
        if (!DarkUtilsConfig.INSTANCE.treeGiftsPerHour) {
            // Prevent leaking samples if feature is turned off after using it
            TreeGiftsPerHour.reset();
            return;
        }

        final var client = MinecraftClient.getInstance();

        // Skip if in main menu or if no trees cut yet
        if (null == client.player || 0L == TreeGiftsPerHour.lastGiftTime) {
            return;
        }

        // Skip if last gift was more than 1 minute ago
        final var now = System.nanoTime();
        if (now - TreeGiftsPerHour.lastGiftTime > TreeGiftsPerHour.ONE_MINUTE_NANOS) {
            return;
        }

        // Skip if gifts per hour is negative or zero, or it's a very big value (max double value)
        final var perHour = TreeGiftsPerHour.getGiftsPerHour();

        if (0.0 >= perHour || MathUtils.isNearEqual(Double.MAX_VALUE, perHour)) { // overflow protection
            return;
        }

        // Skip if not in galatea (Only relevant foraging island for this feature at the moment)
        if (!LocationUtils.isInGalatea()) {
            return;
        }

        final var text = "Tree Gifts/Hour: " + MathUtils.round(perHour, RoundingMode.HALF_DOWN);

        RenderUtils.renderItem(
                context,
                Items.OAK_LOG,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y,
                Formatting.DARK_GREEN
        );
    }

    private static final void onTreeGift(@NotNull final ObtainTreeGiftEvent event) {
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
