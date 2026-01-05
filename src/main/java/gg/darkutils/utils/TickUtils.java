package gg.darkutils.utils;

import gg.darkutils.events.ServerTickEvent;
import gg.darkutils.events.base.EventRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        TickUtils.tasks.removeIf(TickUtils.Task::tick);
    }

    private static final void processAwaitingServerTasks(@NotNull final ServerTickEvent event) {
        TickUtils.serverTasks.removeIf(TickUtils.Task::tick);
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
     *
     * @param condition The condition.
     * @param action    The action.
     */
    public static final void awaitCondition(@NotNull final BooleanSupplier condition, @NotNull final Runnable action) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(action, "action");

        if (condition.getAsBoolean()) {
            action.run();
        } else {
            TickUtils.tasks.add(new TickUtils.Task(condition, action));
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
        Objects.requireNonNull(task, "task");
        if (0 == interval) {
            throw new IllegalArgumentException("Queueing a repeating tick task with interval zero is prohibited");
        }
        TickUtils.tasks.add(new TickUtils.Task(task, interval, true));
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
        Objects.requireNonNull(task, "task");
        if (0 == interval) {
            throw new IllegalArgumentException("Queueing a repeating tick task with interval zero is prohibited");
        }
        TickUtils.serverTasks.add(new TickUtils.Task(task, interval, true));
    }

    /**
     * Queues an updating condition. The given condition will be wrapped to return the same value till it is updated
     * each tick. This is useful for making conditions update each tick and using them each frame.
     *
     * @param condition The interval, in ticks. 20 ticks is considered equal to a second.
     */
    @NotNull
    public static final BooleanSupplier queueUpdatingCondition(@NotNull final BooleanSupplier condition) {
        Objects.requireNonNull(condition, "condition");

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
     * Queues a tick task to be run. If delay is zero, it will be run immediately, without checking the current thread.
     * Otherwise, it will run on the render thread with the specified delay in ticks.
     *
     * @param task  The task.
     * @param delay The delay, in ticks. 20 ticks is considered equal to a second.
     */
    public static final void queueTickTask(@NotNull final Runnable task, final int delay) {
        Objects.requireNonNull(task, "task");
        if (0 == delay) {
            task.run();
        } else {
            TickUtils.tasks.add(new TickUtils.Task(task, delay, false));
        }
    }

    /**
     * Queues a server tick task to be run. If delay is zero, it will be run immediately, without checking the current thread.
     * Otherwise, it will run on the render thread with the specified delay in server ticks.
     *
     * @param task  The task.
     * @param delay The delay, in server ticks. 20 ticks is considered equal to a second.
     */
    public static final void queueServerTickTask(@NotNull final Runnable task, final int delay) {
        Objects.requireNonNull(task, "task");
        if (0 == delay) {
            task.run();
        } else {
            TickUtils.serverTasks.add(new TickUtils.Task(task, delay, false));
        }
    }

    // ============================================================
    // Task system
    // ============================================================

    private static final class Task {
        private final @NotNull Runnable action;
        private final int initialTicks;
        private final boolean repeats;
        private final @Nullable BooleanSupplier condition;
        private int ticks;

        /**
         * Condition-based constructor.
         */
        private Task(@NotNull final BooleanSupplier condition, @NotNull final Runnable action) {
            super();

            this.condition = condition;
            this.action = action;
            this.initialTicks = -1;
            this.ticks = -1;
            this.repeats = false;
        }

        /**
         * Time-based constructor.
         */
        private Task(@NotNull final Runnable action, final int initialTicks, final boolean repeats) {
            super();

            if (0 >= initialTicks) {
                throw new IllegalArgumentException("Task interval must be greater than zero");
            }

            this.condition = null;
            this.action = action;
            this.initialTicks = initialTicks;
            this.ticks = initialTicks;
            this.repeats = repeats;
        }

        /**
         * Ticks the task, running it if it should.
         *
         * @return true If the task should be removed.
         */
        private final boolean tick() {
            if (null != this.condition) {
                if (this.condition.getAsBoolean()) {
                    this.action.run();
                    return true; // remove once condition passes
                }

                return false; // keep until condition is true
            }

            if (1 >= this.ticks) {
                this.action.run();

                if (this.repeats) {
                    this.ticks = this.initialTicks;
                    return false; // keep repeating
                }

                return true; // one-shot, remove
            }

            this.ticks--;
            return false;
        }

        @Override
        public final String toString() {
            return "Task{" +
                    "action=" + this.action +
                    ", initialTicks=" + this.initialTicks +
                    ", repeats=" + this.repeats +
                    ", condition=" + this.condition +
                    ", ticks=" + this.ticks +
                    '}';
        }
    }
}
