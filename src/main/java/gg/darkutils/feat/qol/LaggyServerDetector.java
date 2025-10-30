package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> LaggyServerDetector.onWorldChange(world));
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.laggyServerDetector;
    }

    private static final void onWorldChange(@Nullable final ClientWorld world) {
        if (!LaggyServerDetector.isEnabled() || null == world/* || world.isClient*/) {
            return;
        }

        Arrays.fill(LaggyServerDetector.TPS_SAMPLES, 0);
        LaggyServerDetector.currentIndex = 0;

        ServerTPSCalculator.startCalculatingTPS(LaggyServerDetector::onTPSUpdate);
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

            var comment = "";
            var color = "";

            if (19.0 <= tpsAverageOver30Seconds) {
                comment = " (Good)";
                color = "§a";
            } else if (18.0 <= tpsAverageOver30Seconds) {
                comment = " (Decent)";
                color = "§2";
            } else if (17.0 <= tpsAverageOver30Seconds) {
                comment = " (Fine)";
                color = "§6";
            } else if (16.0 <= tpsAverageOver30Seconds) {
                comment = " (Bearable)";
                color = "§e";
            } else if (15.0 <= tpsAverageOver30Seconds) {
                comment = " (Bad)";
                color = "§c";
            } else {
                comment = " (Very Bad)";
                color = "§4";
            }

            final var client = MinecraftClient.getInstance();
            final var player = client.player;

            if (null == player) {
                return;
            }

            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

            // Required or else would display too much precision, bad for human readability.
            final var tpsAverageOver30SecondsFormatted = String.format(Locale.ROOT, "%.2f", tpsAverageOver30Seconds);

            client.inGameHud.setTitle(Text.of("§d30s TPS AVG: " + color + tpsAverageOver30SecondsFormatted + comment));
            client.inGameHud.setTitleTicks(10, 70, 20);
        }
    }
}
