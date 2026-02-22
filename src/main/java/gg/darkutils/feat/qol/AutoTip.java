package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventPriority;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.SimpleColor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class AutoTip {
    private static final long QUARTER_MINUTES_NANOS = TimeUnit.MINUTES.toNanos(15L);
    @NotNull
    private static final Consumer<ReceiveGameMessageEvent> MESSAGE_ACTION = event -> {
        if (event.isStyledWith(SimpleColor.RED)) {
            event.cancellationState().cancel();
        }
    };
    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
            "You already tipped everyone that has boosters active, so there isn't anybody to be tipped right now!", AutoTip.MESSAGE_ACTION,
            "No one has a network booster active right now! Try again later.", AutoTip.MESSAGE_ACTION
    );
    private static long lastTipAt;

    private AutoTip() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(AutoTip::onChat, EventPriority.ABOVE_NORMAL);
        ClientTickEvents.END_CLIENT_TICK.register(AutoTip::onTick);
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.autoTip || !LocationUtils.isInHypixel()) {
            return;
        }

        event.match(AutoTip.MESSAGE_HANDLERS);
    }

    private static final void onTick(@NotNull final MinecraftClient client) {
        if (!DarkUtilsConfig.INSTANCE.autoTip || !LocationUtils.isInHypixel()) {
            return;
        }

        final var now = 0L == AutoTip.lastTipAt ? 0L : System.nanoTime();

        if (0L == AutoTip.lastTipAt || now - AutoTip.lastTipAt > AutoTip.QUARTER_MINUTES_NANOS) {
            AutoTip.lastTipAt = 0L == AutoTip.lastTipAt ? System.nanoTime() : now;
            ChatUtils.addToSendMessageQueue("/tip all");
        }
    }
}
