package gg.darkutils.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TickUtils {
    private static final @NotNull ConcurrentHashMap<BooleanSupplier, Runnable> conditionals = new ConcurrentHashMap<>(1);
    private static final @NotNull ConcurrentHashMap<Runnable, Integer> tasks = new ConcurrentHashMap<>(1);
    private static final @NotNull Supplier<ClientPlayerEntity> localPlayer = () -> MinecraftClient.getInstance().player;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> TickUtils.processAwaitingConditionals());
        ClientTickEvents.END_CLIENT_TICK.register(client -> TickUtils.processAwaitingTickTasks());
    }

    private TickUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final void processAwaitingConditionals() {
        TickUtils.conditionals.forEach((condition, value) -> {
            if (condition.getAsBoolean()) {
                // remove first, then run (prevents reentrancy bugs)
                final var action = TickUtils.conditionals.remove(condition);
                if (null != action) {
                    action.run();
                }
            }
        });
    }

    private static final void processAwaitingTickTasks() {
        TickUtils.tasks.forEach((task, delay) -> {
            if (1 >= delay) {
                // remove first, then run (prevents reentrancy bugs)
                if (null != TickUtils.tasks.remove(task)) {
                    task.run();
                }
            } else {
                TickUtils.tasks.replace(task, delay - 1);
            }
        });
    }

    /**
     * Awaits a condition then runs an action. The condition is polled every tick. If the condition is initially the true, the action is ran instantly.
     *
     * @param condition The condition.
     * @param action    The action.
     */
    public static final void awaitCondition(final BooleanSupplier condition, @NotNull final Runnable action) {
        if (condition.getAsBoolean()) {
            action.run();
        } else {
            TickUtils.conditionals.put(condition, action);
        }
    }

    /**
     * Awaits the local player joining a world/realm/server then runs the given action passing the player as argument.
     * If the player is initially available, the task will run instantly on the caller thread.
     * Otherwise, it will run on the Render thread when player starts existing. (e.g. joining a world/realm/server)
     *
     * @param action The action.
     */
    public static final void awaitLocalPlayer(@NotNull final Consumer<ClientPlayerEntity> action) {
        // Array wrapping to bypass final variable requirement inside the lambda
        final var player = new ClientPlayerEntity[]{TickUtils.localPlayer.get()};

        // The player will not be null once they join a (singleplayer) world, (dedicated) server or realm.
        TickUtils.awaitCondition(() -> null != (player[0] = TickUtils.localPlayer.get()), () -> action.accept(player[0]));
    }

    /**
     * Queues a tick task to be run. If delay is zero, it will be run immediately, without checking the current thread.
     * Otherwise, it will run on the render thread with the specified delay in ticks.
     *
     * @param task  The task.
     * @param delay The delay, in ticks. 20 ticks is considered equal to a second.
     */
    public static final void queueTickTask(@NotNull final Runnable task, final int delay) {
        if (0 == delay) {
            task.run();
        } else {
            TickUtils.tasks.put(task, delay);
        }
    }
}
