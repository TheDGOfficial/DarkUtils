package gg.darkutils.feat.dungeons;

import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.chat.BasicColor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

public final class DungeonTimer {
    public static long bossEntryTime;
    public static long phase2ClearTime;
    public static long phase4ClearTime;
    public static long phase5ClearTime;

    @NotNull
    private static final Consumer<ReceiveGameMessageEvent> PHASE_5_FINISH = event -> {
        if (0L == DungeonTimer.phase5ClearTime && event.isStyledWith(BasicColor.DARK_RED)) {
            DungeonTimer.phase5ClearTime = System.nanoTime();
        }
    };

    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
            "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!", event -> {
                if (0L == DungeonTimer.bossEntryTime && event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.bossEntryTime = System.nanoTime();
                }
            },
            "[BOSS] Goldor: Who dares trespass into my domain?", event -> {
                if (0L == DungeonTimer.phase2ClearTime && event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.phase2ClearTime = System.nanoTime();
                }
            },
            "[BOSS] Necron: All this, for nothing...", event -> {
                if (0L == DungeonTimer.phase4ClearTime && event.isStyledWith(BasicColor.DARK_RED)) {
                    DungeonTimer.phase4ClearTime = System.nanoTime();
                }
            },
            "[BOSS] Wither King: Incredible. You did what I couldn't do myself.", DungeonTimer.PHASE_5_FINISH,
            "[BOSS] Wither King: Thank you for coming all the way here.", DungeonTimer.PHASE_5_FINISH
    );

    private DungeonTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(DungeonTimer::onChat);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> DungeonTimer.reset());
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        event.match(DungeonTimer.MESSAGE_HANDLERS);
    }

    private static final void reset() {
        DungeonTimer.bossEntryTime = 0L;
        DungeonTimer.phase2ClearTime = 0L;
        DungeonTimer.phase4ClearTime = 0L;
        DungeonTimer.phase5ClearTime = 0L;
    }
}
