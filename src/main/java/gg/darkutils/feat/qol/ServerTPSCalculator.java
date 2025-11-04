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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public final class ServerTPSCalculator {
    private static final @NotNull AtomicInteger tickCount = new AtomicInteger();
    private static final @NotNull ScheduledExecutorService calculatorThread =
            Executors.newSingleThreadScheduledExecutor(r -> Thread.ofPlatform()
                    .name("DarkUtils Server TPS Calculator Thread")
                    .unstarted(r));
    private static final @NotNull AtomicBoolean initialized = new AtomicBoolean();
    private static final @NotNull AtomicInteger lastTPS = new AtomicInteger();
    private static final @NotNull AtomicBoolean enableTPSCalculationTemporarily = new AtomicBoolean();
    private static final @NotNull AtomicReference<@Nullable IntConsumer> hook = new AtomicReference<>();

    static {
        ServerTPSCalculator.calculatorThread.scheduleWithFixedDelay(() -> {
            if (ServerTPSCalculator.shouldCalculate()) {
                final var ticksThisSecond = ServerTPSCalculator.tickCount.getAndSet(0);
                ServerTPSCalculator.lastTPS.getAndSet(Math.min(20, ticksThisSecond));

                if (!ServerTPSCalculator.initialized.get() && 0 < ticksThisSecond) {
                    ServerTPSCalculator.initialized.getAndSet(true);
                }

                if (ServerTPSCalculator.initialized.get()) {
                    final var hookRef = ServerTPSCalculator.hook;
                    final var hook = hookRef.get();
                    if (null != hook) {
                        // Run the hook inside client thread for thread safety.
                        TickUtils.queueTickTask(() -> hook.accept(ServerTPSCalculator.lastTPS.get()), 1);
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
        return ServerTPSCalculator.initialized.get() ? ServerTPSCalculator.lastTPS.get() : -1;
    }

    private static final void onWorldUnload() {
        if (ServerTPSCalculator.shouldCalculate()) {
            ServerTPSCalculator.initialized.getAndSet(false);
            ServerTPSCalculator.lastTPS.getAndSet(0);
            ServerTPSCalculator.tickCount.set(0);
        }
    }

    private static final void onServerTick(@NotNull final ServerTickEvent event) {
        if (ServerTPSCalculator.shouldCalculate()) {
            ServerTPSCalculator.tickCount.incrementAndGet();
        }
    }

    static final void startCalculatingTPS(@NotNull final IntConsumer hook) {
        ServerTPSCalculator.enableTPSCalculationTemporarily.getAndSet(true);
        ServerTPSCalculator.hook.updateAndGet(h -> hook);
    }

    static final void stopCalculatingTPS() {
        ServerTPSCalculator.enableTPSCalculationTemporarily.getAndSet(false);
        ServerTPSCalculator.hook.updateAndGet(h -> null);
    }

    private static final boolean shouldCalculate() {
        return ServerTPSCalculator.enableTPSCalculationTemporarily.get();
    }
}
