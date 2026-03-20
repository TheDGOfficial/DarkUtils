package gg.darkutils.feat.farming;

import gg.darkutils.utils.chat.ChatUtils;

import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;

public final class FarmingState {
    private static final long TEN_SECONDS_NANOS = TimeUnit.SECONDS.toNanos(10L);

    private static long lastFarmingXp;

    @NotNull
    private static final BooleanSupplier ACTIVELY_FARMING =
            TickUtils.queueUpdatingCondition(FarmingState::isActivelyFarmingRightNow);

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
        return FarmingState.ACTIVELY_FARMING.getAsBoolean();
    }

    private static final boolean isActivelyFarmingRightNow() {
        return System.nanoTime() - FarmingState.lastFarmingXp < TEN_SECONDS_NANOS;
    }
}
