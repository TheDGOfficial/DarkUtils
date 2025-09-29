package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.ChatUtils;
import gg.darkutils.utils.Helpers;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class SoloCrushTimer {
    private static boolean firstLightningReceived;
    private static boolean done;

    private SoloCrushTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                SoloCrushTimer.onChat(message);
            }
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            SoloCrushTimer.firstLightningReceived = false;
            SoloCrushTimer.done = false;
        });
    }

    private static final void onChat(@NotNull final Text message) {
        if (!DarkUtilsConfig.INSTANCE.soloCrushTimer) {
            return;
        }

        if (!SoloCrushTimer.done && message.getString().contains("Storm's Giga Lightning hit you for ") && ChatUtils.hasFormatting(message, Formatting.GRAY, false)) {
            if (SoloCrushTimer.firstLightningReceived) {
                SoloCrushTimer.firstLightningReceived = false;
                SoloCrushTimer.done = true;

                Helpers.displayCountdownTitles("§5", "Crush!", 3);
            } else {
                SoloCrushTimer.firstLightningReceived = true;
            }
        }
    }
}
