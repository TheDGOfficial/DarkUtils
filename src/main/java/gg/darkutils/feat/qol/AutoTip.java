package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.chat.ChatUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class AutoTip {
    private static long lastTipAt;

    private AutoTip() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> overlay || !AutoTip.shouldHideMessage(message));
        ClientTickEvents.END_CLIENT_TICK.register(client -> AutoTip.onTick());
    }

    private static final boolean shouldHideMessage(@NotNull final Text message) {
        final var plain = message.getString();

        return DarkUtilsConfig.INSTANCE.autoTip && ("You already tipped everyone that has boosters active, so there isn't anybody to be tipped right now!".equals(plain) || "No one has a network booster active right now! Try again later.".equals(plain)) && ChatUtils.hasFormatting(message, Formatting.RED, false);
    }

    private static final void onTick() {
        if (!DarkUtilsConfig.INSTANCE.autoTip || !LocationUtils.isInHypixel()) {
            return;
        }

        final var now = System.nanoTime();

        if (0L == AutoTip.lastTipAt || now - AutoTip.lastTipAt > TimeUnit.MINUTES.toNanos(15L)) {
            AutoTip.lastTipAt = now;
            ChatUtils.queueUserSentMessageOrCommand("/tip all");
        }
    }
}
