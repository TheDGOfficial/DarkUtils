package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.chat.ChatUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class DialogueSkipTimer {
    private DialogueSkipTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                DialogueSkipTimer.onChat(message);
            }
        });
    }

    private static final void onChat(@NotNull final Text message) {
        if (!DarkUtilsConfig.INSTANCE.dialogueSkipTimer) {
            return;
        }

        final var plain = message.getString();

        if ("[BOSS] The Watcher: Let's see how you can handle this.".equals(plain) && ChatUtils.hasFormatting(message, Formatting.RED, false)) {
            Helpers.displayCountdownTitles("§4", "Kill Blood Mobs!", 3);
        }
    }
}
