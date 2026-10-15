package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.SimpleColor;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.function.Consumer;

public final class DialogueSkipTimer {
    @NotNull
    private static final Consumer<ReceiveGameMessageEvent> BLOOD_START = event -> {
        if (event.isStyledWith(SimpleColor.RED)) {
            TickUtils.queueServerTickTask(() -> Helpers.displayCountdownTitlesInServerTicks("§4", "Kill Blood Mobs!", 3), 340);
        }
    };
    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
        "[BOSS] The Watcher: Congratulations, you made it through the Entrance.", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: Ah, you've finally arrived.", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: Ah, we meet again...", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: So you made it this far... interesting.", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: You've managed to scratch and claw your way here, eh?", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: I'm starting to get tired of seeing you around here...", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: Oh.. hello?", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: Things feel a little more roomy now, eh?", DialogueSkipTimer.BLOOD_START,
        "[BOSS] The Watcher: Ah, we meet again. As I foresaw...", DialogueSkipTimer.BLOOD_START
    );

    private DialogueSkipTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(DialogueSkipTimer::onChat);
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.dialogueSkipTimer || !LocationUtils.isInDungeons()) {
            return;
        }

        event.match(DialogueSkipTimer.MESSAGE_HANDLERS);
    }
}
