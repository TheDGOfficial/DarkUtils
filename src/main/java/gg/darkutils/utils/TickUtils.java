package gg.darkutils.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.darkutils.events.ServerTickEvent;
import gg.darkutils.events.base.EventRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TickUtils {
    private static final @NotNull Set<TickUtils.Task> tasks = ConcurrentHashMap.newKeySet();
    private static final @NotNull Set<TickUtils.Task> serverTasks = ConcurrentHashMap.newKeySet();

    private static final @NotNull Supplier<ClientPlayerEntity> localPlayer = () -> MinecraftClient.getInstance().player;

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> TickUtils.processAwaitingTasks());
        EventRegistry.centralRegistry().addListener(TickUtils::processAwaitingServerTasks);
    }

    private TickUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    // ============================================================
    // Processing
    // ============================================================

    private static final void processAwaitingTasks() {
        TickUtils.processAwaitingTasksIn(TickUtils.tasks);
    }

    private static final void processAwaitingServerTasks(@NotNull final ServerTickEvent event) {
        TickUtils.processAwaitingTasksIn(TickUtils.serverTasks);
    }

    private static final void processAwaitingTasksIn(@NotNull final Set<TickUtils.Task> tasks) {
        tasks.removeIf(TickUtils.Task::tick);
    }

    // ============================================================
    // Public API
    // ============================================================

    /**
     * Awaits a negated condition then runs an action. The condition is polled every tick.
     * If the negated condition is initially true, the action is run instantly.
     * <p>
     * This is a convenience shortcut method to allow using method references, for example:
     * {@snippet :
     * TickUtils.awaitNegatedCondition(LocationUtils::isInDungeons, () -> {});
     *}
     * would run when isInDungeons returns false rather than true, e.g., when leaving dungeons.
     *
     * @param condition The condition before negating.
     * @param action    The action.
     */
    public static final void awaitNegatedCondition(@NotNull final BooleanSupplier condition, @NotNull final Runnable action) {
        TickUtils.awaitCondition(() -> !condition.getAsBoolean(), action);
    }

    /**
     * Awaits a condition then runs an action. The condition is polled every tick.
     * If the condition is initially true, the action is run instantly.
     * <p>
     * A check will be performed to ensure correct caller thread.
     *
     * @param condition The condition.
     * @param action    The action.
     */
    public static final void awaitCondition(@NotNull final BooleanSupplier condition, @NotNull final Runnable action) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(action, "action");

        TickUtils.checkCallerThread();

        if (condition.getAsBoolean()) {
            action.run();
        } else {
            TickUtils.tasks.add(new TickUtils.ConditionalTask(condition, action));
        }
    }

    /**
     * Awaits the local player joining a world/realm/server then runs the given action passing the player as argument.
     * <p>
     * If the player is initially available, the task will run instantly.
     * Otherwise, it will run when the player starts existing. (e.g. joining a world/realm/server)
     * <p>
     * If calling from outside the Render thread, a task will be queued even if the player is initially available, to
     * ensure thread-safety.
     *
     * @param action The action.
     */
    public static final void awaitLocalPlayer(@NotNull final Consumer<ClientPlayerEntity> action) {
        Objects.requireNonNull(action, "action");

        if (TickUtils.isNotCallingFromRenderThread()) {
            TickUtils.queueTickTask(() -> TickUtils.awaitLocalPlayerInternal(action), 1);
            return;
        }

        TickUtils.awaitLocalPlayerInternal(action);
    }

    /**
     * Awaits the local player joining a world/realm/server then runs the given action passing the player as argument.
     * <p>
     * If the player is initially available, the task will run instantly.
     * Otherwise, it will run when the player starts existing. (e.g. joining a world/realm/server)
     * <p>
     * If calling from outside the Render thread, an exception will be thrown.
     *
     * @param action The action.
     */
    private static final void awaitLocalPlayerInternal(@NotNull final Consumer<ClientPlayerEntity> action) {
        TickUtils.checkCallerThread();

        Objects.requireNonNull(action, "action");

        // Array wrapping to bypass final variable requirement inside the lambda
        final var player = new ClientPlayerEntity[]{TickUtils.localPlayer.get()};

        // The player will not be null once they join a (singleplayer) world, (dedicated) server or realm.
        TickUtils.awaitCondition(() -> null != (player[0] = TickUtils.localPlayer.get()), () -> action.accept(player[0]));
    }

    /**
     * Queues a repeating tick task to be run with the given interval. If interval is zero, an exception will be thrown.
     * Otherwise, it will run on the render thread every interval amount of ticks, e.g., interval = 1 runs it every tick,
     * interval = 2 runs it every other tick, and so on.
     *
     * @param task     The task.
     * @param interval The interval, in ticks. 20 ticks is considered equal to a second.
     */
    public static final void queueRepeatingTickTask(@NotNull final Runnable task, final int interval) {
        TickUtils.queueRepeatingTickTaskInternal(task, interval, true);
    }

    /**
     * Queues a repeating server tick task to be run with the given interval. If interval is zero, an exception will be thrown.
     * Otherwise, it will run on the render thread every interval amount of server ticks, e.g., interval = 1 runs it every server tick,
     * interval = 2 runs it every other server tick, and so on.
     *
     * @param task     The task.
     * @param interval The interval, in server ticks. 20 ticks is considered equal to a second if the server is running at 20 tps with no lag.
     */
    public static final void queueRepeatingServerTickTask(@NotNull final Runnable task, final int interval) {
        TickUtils.queueRepeatingTickTaskInternal(task, interval, false);
    }

    /**
     * Queues a repeating tick task to be run with the given interval. If interval is zero, an exception will be thrown.
     * Otherwise, it will run on the render thread every interval amount of client or server ticks, e.g., interval = 1 runs it every tick,
     * interval = 2 runs it every other tick, and so on.
     *
     * @param task     The task.
     * @param interval The interval, in ticks. 20 ticks is considered equal to a second.
     * @param client   Pass true to use client ticks, false to use server ticks.
     */
    private static final void queueRepeatingTickTaskInternal(@NotNull final Runnable task, final int interval, final boolean client) {
        Objects.requireNonNull(task, "task");
        if (0 == interval) {
            throw new IllegalArgumentException("Queueing a repeating tick task with interval zero is prohibited");
        }
        (client ? TickUtils.tasks : TickUtils.serverTasks).add(new TickUtils.RepeatingTask(task, interval));
    }

    /**
     * Queues an updating condition. The given condition will be wrapped to return the same value till it is updated
     * each tick. This is useful for making conditions update each tick and using them each frame.
     * <p>
     * If the calling thread is not the render thread, an exception will be thrown, as we call .update initially
     * on the caller thread to ensure no uninitialized reads happen.
     *
     * @param condition The condition that will be updated one time initially and then 20 times every second.
     */
    @NotNull
    public static final BooleanSupplier queueUpdatingCondition(@NotNull final BooleanSupplier condition) {
        Objects.requireNonNull(condition, "condition");

        TickUtils.checkCallerThread();

        final class CachedCondition implements BooleanSupplier {
            private boolean value;

            private CachedCondition() {
                super();
            }

            @Override
            public final boolean getAsBoolean() {
                return this.value;
            }

            private final void update() {
                this.value = condition.getAsBoolean();
            }

            @Override
            public final String toString() {
                return "CachedCondition{" +
                        "value=" + this.value +
                        '}';
            }
        }

        final var cached = new CachedCondition();

        // Initialize immediately so the first read is correct
        cached.update();

        // Update once per tick
        TickUtils.queueRepeatingTickTask(cached::update, 1);

        return cached;
    }

    /**
     * Queues a tick task to be run. If delay is zero, it will be run immediately, with a check to ensure correct threading.
     * Otherwise, it will run on the render thread with the specified delay in ticks.
     *
     * @param task  The task.
     * @param delay The delay, in ticks. 20 ticks is considered equal to a second.
     */
    public static final void queueTickTask(@NotNull final Runnable task, final int delay) {
        TickUtils.queueTickTaskInternal(task, delay, true);
    }

    /**
     * Queues a server tick task to be run. If delay is zero, it will be run immediately, with a check to ensure correct threading.
     * Otherwise, it will run on the render thread with the specified delay in server ticks.
     *
     * @param task  The task.
     * @param delay The delay, in server ticks. 20 ticks is considered equal to a second.
     */
    public static final void queueServerTickTask(@NotNull final Runnable task, final int delay) {
        TickUtils.queueTickTaskInternal(task, delay, false);
    }

    /**
     * Queues tick task to be run. If delay is zero, it will be run immediately, with a check to ensure correct threading.
     * Otherwise, it will run on the render thread with the specified delay in either client or server ticks, depending on the parameter.
     *
     * @param task   The task.
     * @param delay  The delay, in client or server ticks. 20 ticks is considered equal to a second.
     * @param client Pass true to use client ticks, false to use server ticks.
     */
    private static final void queueTickTaskInternal(@NotNull final Runnable task, final int delay, final boolean client) {
        Objects.requireNonNull(task, "task");
        if (0 == delay) {
            TickUtils.checkCallerThread();
            task.run();
        } else {
            (client ? TickUtils.tasks : TickUtils.serverTasks).add(new TickUtils.OneShotTask(task, delay));
        }
    }

    private static final boolean isNotCallingFromRenderThread() {
        return !RenderSystem.isOnRenderThread();
    }

    private static final void checkCallerThread() {
        if (TickUtils.isNotCallingFromRenderThread()) {
            throw new IllegalStateException("unexpected caller thread with name: " + Thread.currentThread().getName() + ", expected: Render thread");
        }
    }

    // ============================================================
    // Task system
    // ============================================================

    private sealed interface Task
            permits TickUtils.OneShotTask, TickUtils.RepeatingTask, TickUtils.ConditionalTask {
        boolean tick();
    }

    private static final class OneShotTask implements TickUtils.Task {
        private final @NotNull Runnable action;
        private int ticks;

        private OneShotTask(@NotNull final Runnable action, final int delay) {
            super();

            if (0 >= delay) {
                throw new IllegalArgumentException("delay must be > 0");
            }
            this.action = action;
            this.ticks = delay;
        }

        @Override
        public final boolean tick() {
            if (0 >= --this.ticks) {
                this.action.run();
                return true; // remove
            }
            return false;
        }

        @Override
        public final String toString() {
            return "OneShotTask{ticks=" + this.ticks + ", action=" + this.action + '}';
        }
    }

    private static final class RepeatingTask implements TickUtils.Task {
        private final @NotNull Runnable action;
        private final int interval;
        private int ticks;

        private RepeatingTask(@NotNull final Runnable action, final int interval) {
            super();

            if (0 >= interval) {
                throw new IllegalArgumentException("interval must be > 0");
            }
            this.action = action;
            this.interval = interval;
            this.ticks = interval;
        }

        @Override
        public final boolean tick() {
            if (0 >= --this.ticks) {
                this.action.run();
                this.ticks = this.interval;
            }
            return false; // never removed
        }

        @Override
        public final String toString() {
            return "RepeatingTask{ticks=" + this.ticks + ", interval=" + this.interval + ", action=" + this.action + '}';
        }
    }

    private record ConditionalTask(@NotNull BooleanSupplier condition,
                                   @NotNull Runnable action) implements TickUtils.Task {
        @Override
        public final boolean tick() {
            if (this.condition.getAsBoolean()) {
                this.action.run();
                return true; // remove once condition passes
            }
            return false;
        }

        @Override
        public final @NotNull String toString() {
            return "ConditionalTask{condition=" + this.condition + ", action=" + this.action + '}';
        }
    }
}
