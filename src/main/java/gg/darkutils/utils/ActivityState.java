package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;

import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;

public final class ActivityState {
    private static final long TEN_SECONDS_NANOS = TimeUnit.SECONDS.toNanos(10L);

    private static long lastFarmingXp;
    private static long lastMiningXp;

    @NotNull
    private static final BooleanSupplier ACTIVELY_FARMING =
            TickUtils.queueUpdatingCondition(ActivityState::isActivelyFarmingRightNow);

    @NotNull
    private static final BooleanSupplier ACTIVELY_MINING =
            TickUtils.queueUpdatingCondition(ActivityState::isActivelyMiningRightNow);

    private ActivityState() {
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
                ActivityState.lastFarmingXp = System.nanoTime();
            } else if (text.contains(" Mining (")) {
                ActivityState.lastMiningXp = System.nanoTime();
            }
        });
    }

    public static final boolean isActivelyFarming() {
        return ActivityState.ACTIVELY_FARMING.getAsBoolean();
    }

    private static final boolean isActivelyFarmingRightNow() {
        return ActivityState.isActive(ActivityState.lastFarmingXp);
    }

    public static final boolean isActivelyMining() {
        return ActivityState.ACTIVELY_MINING.getAsBoolean();
    }

    private static final boolean isActivelyMiningRightNow() {
        return ActivityState.isActive(ActivityState.lastMiningXp);
    }

    private static final boolean isActive(final long last) {
        return System.nanoTime() - last < TEN_SECONDS_NANOS;
    }
}
