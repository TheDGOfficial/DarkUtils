package gg.darkutils.feat.qol;

import gg.darkutils.events.ServerTickEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

public final class ServerTPSCalculator {
    private static boolean enableTPSCalculationTemporarily;
    private static boolean initialized;

    private static int tickCount;
    private static int lastTPS;

    private static @Nullable IntConsumer hook;

    private ServerTPSCalculator() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    private static final void calculateTPS() {
        if (!ServerTPSCalculator.enableTPSCalculationTemporarily) {
            return;
        }

        final var ticksThisSecond = ServerTPSCalculator.tickCount;
        ServerTPSCalculator.tickCount = 0;

        ServerTPSCalculator.lastTPS = Math.min(20, ticksThisSecond);

        var initialized = ServerTPSCalculator.initialized;

        if (!initialized && 0 < ticksThisSecond) {
            ServerTPSCalculator.initialized = true;
            initialized = true;
        }

        if (initialized) {
            final var hook = ServerTPSCalculator.hook;
            if (null != hook) {
                hook.accept(ServerTPSCalculator.lastTPS);
            }
        }
    }

    public static final void init() {
        TickUtils.queueRepeatingTickTask(ServerTPSCalculator::calculateTPS, 20);

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(ServerTPSCalculator::onWorldUnload);
        EventRegistry.centralRegistry().addListener(ServerTPSCalculator::onServerTick);
    }

    /**
     * Gets TPS from the last second.
     * Returns -1 if not measured yet.
     */
    public static final int getLastTPS() {
        return ServerTPSCalculator.initialized ? ServerTPSCalculator.lastTPS : -1;
    }

    private static final void resetState() {
        ServerTPSCalculator.initialized = false;
        ServerTPSCalculator.lastTPS = 0;
        ServerTPSCalculator.tickCount = 0;
    }

    private static final void onWorldUnload(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        if (ServerTPSCalculator.enableTPSCalculationTemporarily) {
            ServerTPSCalculator.resetState();
        }
    }

    private static final void onServerTick(@NotNull final ServerTickEvent event) {
        if (ServerTPSCalculator.enableTPSCalculationTemporarily) {
            ++ServerTPSCalculator.tickCount;
        }
    }

    static final void startCalculatingTPS(@NotNull final IntConsumer hook) {
        ServerTPSCalculator.resetState();

        ServerTPSCalculator.hook = hook;
        ServerTPSCalculator.enableTPSCalculationTemporarily = true;
    }

    static final void stopCalculatingTPS() {
        ServerTPSCalculator.hook = null;
        ServerTPSCalculator.enableTPSCalculationTemporarily = false;

        ServerTPSCalculator.resetState();
    }
}
