package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;

public final class LaggyServerDetector {
    private static final int @NotNull [] TPS_SAMPLES = new int[30]; // TPS Over 30 seconds as the sample size
    private static int currentIndex;

    private LaggyServerDetector() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(LaggyServerDetector::onWorldChange);
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.laggyServerDetector;
    }

    private static final void onWorldChange(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        if (!LaggyServerDetector.isEnabled() || LocationUtils.isInSingleplayer()) {
            return;
        }

        Arrays.fill(LaggyServerDetector.TPS_SAMPLES, 0);
        LaggyServerDetector.currentIndex = 0;

        if (null != MinecraftClient.getInstance().getCurrentServerEntry()) {
            ServerTPSCalculator.startCalculatingTPS(LaggyServerDetector::onTPSUpdate);
        }
    }

    private static final @NotNull LaggyServerDetector.TPSCommentAndColor getTpsStatus(final double tps) {
        return 19.0 <= tps ? new LaggyServerDetector.TPSCommentAndColor(" (Good)", "§a")
                : 18.0 <= tps ? new LaggyServerDetector.TPSCommentAndColor(" (Decent)", "§2")
                : 17.0 <= tps ? new LaggyServerDetector.TPSCommentAndColor(" (Fine)", "§6")
                : 16.0 <= tps ? new LaggyServerDetector.TPSCommentAndColor(" (Bearable)", "§e")
                : 15.0 <= tps ? new LaggyServerDetector.TPSCommentAndColor(" (Bad)", "§c")
                : new LaggyServerDetector.TPSCommentAndColor(" (Very Bad)", "§4");
    }

    private static final void onTPSUpdate(final int tps) {
        if (!LaggyServerDetector.isEnabled()) {
            return;
        }

        LaggyServerDetector.TPS_SAMPLES[LaggyServerDetector.currentIndex] = tps;
        LaggyServerDetector.currentIndex++;

        if (LaggyServerDetector.TPS_SAMPLES.length == LaggyServerDetector.currentIndex) {
            LaggyServerDetector.currentIndex = 0;
            ServerTPSCalculator.stopCalculatingTPS();

            var totalTicksOver30Seconds = 0;

            for (final var ticks : LaggyServerDetector.TPS_SAMPLES) {
                totalTicksOver30Seconds += ticks;
            }

            final var tpsAverageOver30Seconds = (double) totalTicksOver30Seconds / LaggyServerDetector.TPS_SAMPLES.length;
            final var tpsStatus = LaggyServerDetector.getTpsStatus(tpsAverageOver30Seconds);

            final var client = MinecraftClient.getInstance();
            final var player = client.player;

            if (null == player) {
                return;
            }

            // Required or else would display too much precision, bad for human readability.
            final var tpsAverageOver30SecondsFormatted = String.format(Locale.ROOT, "%.2f", tpsAverageOver30Seconds);

            client.inGameHud.setTitle(Text.of("§d30s TPS AVG: " + tpsStatus.color() + tpsAverageOver30SecondsFormatted + tpsStatus.comment()));
            client.inGameHud.setTitleTicks(10, 70, 20);
        }
    }

    private record TPSCommentAndColor(@NotNull String comment, @NotNull String color) {
    }
}
