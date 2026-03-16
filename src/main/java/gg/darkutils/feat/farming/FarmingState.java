package gg.darkutils.feat.farming;

import gg.darkutils.utils.chat.ChatUtils;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.concurrent.TimeUnit;

public final class FarmingState {
    private static long lastFarmingXp;

    private FarmingState() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                return;
            }

            final var text = ChatUtils.removeControlCodes(message.getString());

            if (text.contains(" Farming (")) {
                FarmingState.onFarmingXp();
            }
        });
    }

    private static final void onFarmingXp() {
        FarmingState.lastFarmingXp = System.nanoTime();
    }

    public static final boolean isActivelyFarming() {
        return System.nanoTime() - FarmingState.lastFarmingXp < TimeUnit.SECONDS.toNanos(10L);
    }
}
