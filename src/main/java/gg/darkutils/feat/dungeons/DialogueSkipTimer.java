package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.chat.BasicColor;
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

        final var plain = event.content();

        if ("[BOSS] The Watcher: Let's see how you can handle this.".equals(plain) && event.isStyledWith(BasicColor.RED)) {
            Helpers.displayCountdownTitles("§4", "Kill Blood Mobs!", 3);
        }
    }
}
