package gg.darkutils.feat.qol;

import gg.darkutils.events.ServerTickEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public final class ServerTPSCalculator {
    private static final @NotNull AtomicInteger tickCount = new AtomicInteger();
    private static final @NotNull ScheduledExecutorService calculatorThread =
            Executors.newSingleThreadScheduledExecutor(r -> Thread.ofPlatform()
                    .name("DarkUtils Server TPS Calculator Thread")
                    .unstarted(r));
    private static volatile boolean initialized;
    private static volatile int lastTPS;
    private static volatile boolean enableTPSCalculationTemporarily;
    private static @Nullable IntConsumer hook;

    static {
        ServerTPSCalculator.calculatorThread.scheduleWithFixedDelay(() -> {
            if (ServerTPSCalculator.enableTPSCalculationTemporarily) {
                final var ticksThisSecond = ServerTPSCalculator.tickCount.getAndSet(0);
                ServerTPSCalculator.lastTPS = Math.min(20, ticksThisSecond);

                var initialized = ServerTPSCalculator.initialized;

                if (!initialized && 0 < ticksThisSecond) {
                    ServerTPSCalculator.initialized = true;
                    initialized = true;
                }

                if (initialized) {
                    // Run the hook inside client thread for thread safety.
                    TickUtils.queueTickTask(() -> {
                        final var hook = ServerTPSCalculator.hook;
                        if (null != hook) {
                            hook.accept(ServerTPSCalculator.lastTPS);
                        }
                    }, 1);
                }
            }
        }, 1L, 1L, TimeUnit.SECONDS);
    }

    private ServerTPSCalculator() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
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

    private static final void onWorldUnload(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        if (ServerTPSCalculator.enableTPSCalculationTemporarily) {
            ServerTPSCalculator.initialized = false;
            ServerTPSCalculator.lastTPS = 0;
            ServerTPSCalculator.tickCount.set(0);
        }
    }

    private static final void onServerTick(@NotNull final ServerTickEvent event) {
        if (ServerTPSCalculator.enableTPSCalculationTemporarily) {
            ServerTPSCalculator.tickCount.incrementAndGet();
        }
    }

    static final void startCalculatingTPS(@NotNull final IntConsumer hook) {
        ServerTPSCalculator.hook = hook;
        ServerTPSCalculator.enableTPSCalculationTemporarily = true;
    }

    static final void stopCalculatingTPS() {
        ServerTPSCalculator.hook = null;
        ServerTPSCalculator.enableTPSCalculationTemporarily = false;
    }
}
