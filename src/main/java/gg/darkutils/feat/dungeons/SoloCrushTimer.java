package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.chat.SimpleColor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public final class SoloCrushTimer {
    private static boolean firstLightningReceived;
    private static boolean inProgress = true;

    private SoloCrushTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(SoloCrushTimer::onChat);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(SoloCrushTimer::reset);
    }

    private static final void reset(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        SoloCrushTimer.firstLightningReceived = false;
        SoloCrushTimer.inProgress = true;
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.soloCrushTimer || !LocationUtils.isInDungeons()) {
            return;
        }

        if (SoloCrushTimer.inProgress && event.content().startsWith("Storm's Giga Lightning hit you for ") && event.isStyledWith(SimpleColor.GRAY)) {
            if (SoloCrushTimer.firstLightningReceived) {
                SoloCrushTimer.firstLightningReceived = false;
                SoloCrushTimer.inProgress = false;

                Helpers.displayCountdownTitlesInServerTicks("§5", "Crush!", 3);
            } else {
                SoloCrushTimer.firstLightningReceived = true;
            }
        }
    }
}
