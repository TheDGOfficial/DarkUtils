package gg.darkutils.feat.dungeons;

import gg.darkutils.utils.chat.ChatUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public final class DungeonTimer {
    public static long bossEntryTime;
    public static long phase2ClearTime;

    private DungeonTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientReceiveMessageEvents.GAME.register(DungeonTimer::onChat);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> DungeonTimer.reset());
    }

    private static final void onChat(@NotNull final Text message, final boolean overlay) {
        if (overlay) {
            return;
        }

        final var plain = message.getString();
        final var bossMessage = plain.startsWith("[BOSS] ");

        if (0L == DungeonTimer.bossEntryTime && bossMessage && plain.contains(":") && ChatUtils.hasFormatting(message, Formatting.DARK_RED, false)) {
            final var bossName = StringUtils.substringBefore(StringUtils.substringAfter(plain, "[BOSS] "), ":").trim();

            if ("Maxor".equals(bossName)) {
                DungeonTimer.bossEntryTime = System.nanoTime();
            }
        } else if (0L == DungeonTimer.phase2ClearTime && bossMessage && plain.endsWith("Who dares trespass into my domain?") && ChatUtils.hasFormatting(message, Formatting.DARK_RED, false)) {
            DungeonTimer.phase2ClearTime = System.nanoTime();
        }
    }

    private static final void reset() {
        DungeonTimer.bossEntryTime = 0L;
        DungeonTimer.phase2ClearTime = 0L;
    }
}
