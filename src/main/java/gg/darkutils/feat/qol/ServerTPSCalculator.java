package gg.darkutils.feat.qol;

import gg.darkutils.events.ServerTickEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public final class ServerTPSCalculator {
    private static final @NotNull AtomicInteger tickCount = new AtomicInteger();
    private static final @NotNull ScheduledExecutorService calculatorThread = Executors.newSingleThreadScheduledExecutor((@NotNull final Runnable r) -> new Thread(r, "DarkUtils Server TPS Calculator Thread"));
    private static volatile boolean initialized;
    private static volatile int lastTPS;
    private static volatile boolean enableTPSCalculationTemporarily;
    private static final @NotNull AtomicReference<@Nullable IntConsumer> hook = new AtomicReference<>();

    static {
        ServerTPSCalculator.calculatorThread.scheduleWithFixedDelay(() -> {
            if (ServerTPSCalculator.shouldCalculate()) {
                final var ticksThisSecond = ServerTPSCalculator.tickCount.getAndSet(0);
                ServerTPSCalculator.lastTPS = Math.min(20, ticksThisSecond);

                if (!ServerTPSCalculator.initialized && 0 < ticksThisSecond) {
                    ServerTPSCalculator.initialized = true;
                }

                if (ServerTPSCalculator.initialized) {
                    final var hookRef = ServerTPSCalculator.hook;
                    final var hook = hookRef.get();
                    if (null != hook) {
                        final var value = ServerTPSCalculator.lastTPS;

                        // Run the hook inside client thread for thread safety.
                        TickUtils.queueTickTask(() -> hook.accept(value), 1);
                    }
                }
            }
        }, 1L, 1L, TimeUnit.SECONDS);
    }

    private ServerTPSCalculator() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> ServerTPSCalculator.onWorldUnload());
        EventRegistry.centralRegistry().addListener(ServerTPSCalculator::onServerTick);
    }

    /**
     * Gets TPS from the last second.
     * Returns -1 if not measured yet.
     */
    public static final int getLastTPS() {
        return ServerTPSCalculator.initialized ? ServerTPSCalculator.lastTPS : -1;
    }

    private static final void onWorldUnload() {
        if (ServerTPSCalculator.shouldCalculate()) {
            ServerTPSCalculator.initialized = false;
            ServerTPSCalculator.lastTPS = 0;
            ServerTPSCalculator.tickCount.set(0);
        }
    }

    private static final void onServerTick(@NotNull final ServerTickEvent event) {
        if (ServerTPSCalculator.shouldCalculate()) {
            ServerTPSCalculator.tickCount.incrementAndGet();
        }
    }

    static final void startCalculatingTPS(@NotNull final IntConsumer hook) {
        ServerTPSCalculator.enableTPSCalculationTemporarily = true;
        ServerTPSCalculator.hook.updateAndGet(h -> hook);
    }

    static final void stopCalculatingTPS() {
        ServerTPSCalculator.enableTPSCalculationTemporarily = false;
        ServerTPSCalculator.hook.updateAndGet(h -> null);
    }

    private static final boolean shouldCalculate() {
        return ServerTPSCalculator.enableTPSCalculationTemporarily;
    }
}
