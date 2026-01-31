package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.chat.SimpleColor;
import org.jetbrains.annotations.NotNull;

public final class DialogueSkipTimer {
    private DialogueSkipTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(DialogueSkipTimer::onChat);
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.dialogueSkipTimer) {
            return;
        }

        if (event.matches("[BOSS] The Watcher: Let's see how you can handle this.") && event.isStyledWith(SimpleColor.RED)) {
            Helpers.displayCountdownTitlesInServerTicks("§4", "Kill Blood Mobs!", 3);
        }
    }
}
