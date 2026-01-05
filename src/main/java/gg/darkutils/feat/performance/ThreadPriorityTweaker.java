package gg.darkutils.feat.performance;

import gg.darkutils.config.DarkUtilsConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ThreadPriorityTweaker {
    /**
     * Run from separate thread to not lag the game since finding root thread group, then iterating over all threads,
     * and then setting their priorities according to their names is not entirely free, and we have to do this
     * periodically because new threads might spawn at a later time, after we set the priorities for the first
     * time. There's unfortunately no JDK API that triggers when a new thread is spawned, so we must do this.
     */
    @NotNull
    private static final ScheduledExecutorService threadPriorityTweakerScheduler = Executors.newSingleThreadScheduledExecutor(r -> Thread.ofPlatform()
            .name("DarkUtils Thread Priority Tweaker Thread")
            .unstarted(r));

    /**
     * Holds all tweaks.
     */
    private static final @NotNull Set<ThreadPriorityTweaker.ThreadPriorityTweak> tweaks = ThreadPriorityTweaker.getAllTweaks();
    /**
     * Holds only the exact tweaks for O(1) HashMap access.
     */
    private static final @NotNull Map<String, ThreadPriorityTweaker.ThreadPriorityTweak> exacts = ThreadPriorityTweaker.tweaks
            .stream()
            .filter(tweak -> ThreadPriorityTweaker.NameMatcherMode.EXACT == tweak.nameMatcherMode())
            .collect(Collectors.toUnmodifiableMap(ThreadPriorityTweaker.ThreadPriorityTweak::threadName, Function.identity()));
    /**
     * Holds other tweaks that need to do more complex checks than equality.
     * We can't use hash here and so it will be O(n) in terms of algorithmic complexity.
     */
    private static final @NotNull Set<ThreadPriorityTweaker.ThreadPriorityTweak> others = ThreadPriorityTweaker.tweaks
            .stream()
            .filter(tweak -> ThreadPriorityTweaker.NameMatcherMode.EXACT != tweak.nameMatcherMode())
            .collect(Collectors.toUnmodifiableSet());

    private ThreadPriorityTweaker() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    /**
     * Gets all tweaks.
     */
    private static final @NotNull Set<ThreadPriorityTweaker.ThreadPriorityTweak> getAllTweaks() {
        final var set = HashSet.<ThreadPriorityTweaker.ThreadPriorityTweak>newHashSet(32);

        set.addAll(ThreadPriorityTweaker.getCriticalTweaks());
        set.addAll(ThreadPriorityTweaker.getOtherTweaks());

        return Set.copyOf(set);
    }

    private static final @NotNull Set<ThreadPriorityTweaker.ThreadPriorityTweak> getCriticalTweaks() {
        final var crit = ThreadPriorityTweaker.ThreadPriority.CRITICAL;
        final var highest = ThreadPriorityTweaker.ThreadPriority.HIGHEST;
        final var veryHigh = ThreadPriorityTweaker.ThreadPriority.VERY_HIGH;
        final var high = ThreadPriorityTweaker.ThreadPriority.HIGH;
        final var aboveNormal = ThreadPriorityTweaker.ThreadPriority.ABOVE_NORMAL;

        return Set.of(
                // Keep important I/O at the top to not cause unexpected latency increase after turning on the tweaker.
                ThreadPriorityTweaker.startsWith("Netty ", crit),
                ThreadPriorityTweaker.startsWith("Ixeris ", crit),
                ThreadPriorityTweaker.exactMatch("Render thread", highest),
                ThreadPriorityTweaker.startsWith("Chunk Render ", veryHigh),
                ThreadPriorityTweaker.startsWith("c2me", veryHigh),
                ThreadPriorityTweaker.exactMatch("CullThread", high),
                ThreadPriorityTweaker.exactMatch("Reference Handler", high),
                ThreadPriorityTweaker.startsWith("scalablelux", high),
                ThreadPriorityTweaker.startsWith("AsyncParticle", high),
                ThreadPriorityTweaker.exactMatch("Server thread", aboveNormal),
                ThreadPriorityTweaker.startsWith("ALSoft", aboveNormal),
                ThreadPriorityTweaker.exactMatch("Sound engine", aboveNormal),
                ThreadPriorityTweaker.exactMatch("RSLS Scheduler", aboveNormal),
                ThreadPriorityTweaker.startsWith("Server Pinger #", aboveNormal),
                ThreadPriorityTweaker.startsWith("LanServerDetector #", aboveNormal)
        );
    }

    private static final @NotNull Set<ThreadPriorityTweaker.ThreadPriorityTweak> getOtherTweaks() {
        final var low = ThreadPriorityTweaker.ThreadPriority.LOW;
        final var veryLow = ThreadPriorityTweaker.ThreadPriority.VERY_LOW;
        final var lowest = ThreadPriorityTweaker.ThreadPriority.LOWEST;
        final var idle = ThreadPriorityTweaker.ThreadPriority.IDLE;

        return Set.of(
                ThreadPriorityTweaker.exactMatch("Yggdrasil Key Fetcher", low),
                ThreadPriorityTweaker.startsWith("Worker", low),
                ThreadPriorityTweaker.startsWith("IO", low),
                ThreadPriorityTweaker.startsWith("DefaultDispatcher", veryLow),
                ThreadPriorityTweaker.startsWith("ForkJoinPool", veryLow),
                ThreadPriorityTweaker.startsWith("HttpClient", veryLow),
                ThreadPriorityTweaker.startsWith("spark", veryLow),
                ThreadPriorityTweaker.startsWith("Scheduler", veryLow),
                ThreadPriorityTweaker.startsWith("Log4j2", lowest),
                ThreadPriorityTweaker.exactMatch("Timer hack thread", idle),
                ThreadPriorityTweaker.exactMatch("Snooper timer", idle),
                ThreadPriorityTweaker.startsWith("DarkUtils", idle)
        );
    }

    @NotNull
    private static final ThreadPriorityTweaker.ThreadPriorityTweak exactMatch(@NotNull final String threadName, @NotNull final ThreadPriorityTweaker.ThreadPriority priority) {
        return new ThreadPriorityTweaker.ThreadPriorityTweak(threadName, ThreadPriorityTweaker.NameMatcherMode.EXACT, priority);
    }

    @NotNull
    private static final ThreadPriorityTweaker.ThreadPriorityTweak startsWith(@NotNull final String threadNameStart, @NotNull final ThreadPriorityTweaker.ThreadPriority priority) {
        return new ThreadPriorityTweaker.ThreadPriorityTweak(threadNameStart, ThreadPriorityTweaker.NameMatcherMode.STARTS_WITH, priority);
    }

    /**
     * Tweaks the priority of all currently live threads and registers a
     * task that runs every minute to periodically do this operation again,
     * in case new threads are spawned.
     */
    private static final void scheduleTweakTask() {
        ThreadPriorityTweaker.threadPriorityTweakerScheduler.scheduleWithFixedDelay(ThreadPriorityTweaker::tweakPriorities, 0L, 60L, TimeUnit.SECONDS);
    }

    /**
     * Checks if the tweaker is enabled or not in the config.
     */
    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.threadPriorityTweaker;
    }

    public static final void init() {
        ThreadPriorityTweaker.scheduleTweakTask();
    }

    /**
     * Tweaks priorities of currently live threads. This action is done on a separate thread to not cause any lag in-game.
     */
    private static final void tweakPriorities() {
        if (!ThreadPriorityTweaker.isEnabled()) {
            return;
        }

        for (final var thread : ThreadPriorityTweaker.getAllThreads()) {
            final var name = thread.getName();

            if (ThreadPriorityTweaker.tweakPriorityExact(name, thread)) {
                continue;
            }

            ThreadPriorityTweaker.tweakPriority(thread, name);
        }
    }

    private static final boolean tweakPriorityExact(@NotNull final String name, @NotNull final Thread thread) {
        final var exactMatch = ThreadPriorityTweaker.exacts.get(name);

        if (null != exactMatch) {
            ThreadPriorityTweaker.tweakPriority(thread, exactMatch.priority().getPriority());
            return true;
        }

        return false;
    }

    private static final void tweakPriority(@NotNull final Thread thread, @NotNull final String name) {
        var foundMatchingTweaker = false;

        for (final var tweaker : ThreadPriorityTweaker.others) {
            if (tweaker.appliesTo(name)) {
                foundMatchingTweaker = true;

                tweaker.applyTo(thread);
                break;
            }
        }

        if (!foundMatchingTweaker && ThreadPriorityTweaker.ThreadPriority.NORMAL.getPriority() < thread.getPriority()) {
            // Unknown thread names with higher than NORMAL priority gets set back to NORMAL.
            ThreadPriorityTweaker.tweakPriority(thread, ThreadPriorityTweaker.ThreadPriority.NORMAL.getPriority());
        }
    }

    private static final void tweakPriority(@NotNull final Thread thread, final int priority) {
        final var oldPriority = thread.getPriority();

        if (oldPriority != priority) {
            thread.setPriority(priority);
        }
    }

    @NotNull
    private static final ThreadGroup getRootThreadGroup() {
        var threadGroup = Thread.currentThread().getThreadGroup();

        ThreadGroup parent;
        while (null != (parent = threadGroup.getParent())) {
            threadGroup = parent;
        }

        return threadGroup;
    }

    @NotNull
    private static final Thread @NotNull [] getAllThreads() {
        final var threadGroup = ThreadPriorityTweaker.getRootThreadGroup();
        var count = threadGroup.activeCount();

        Thread[] threads;
        do {
            threads = new Thread[count];
            count = threadGroup.enumerate(threads, true);
        } while (count > threads.length);

        var nonNullCount = 0;
        for (final var thread : threads) {
            if (null != thread) {
                threads[nonNullCount] = thread;
                ++nonNullCount;
            }
        }

        return Arrays.copyOf(threads, nonNullCount);
    }

    private enum NameMatcherMode {
        EXACT(String::equals),
        STARTS_WITH(String::startsWith);

        @NotNull
        private final BiPredicate<String, String> matcher;

        private NameMatcherMode(@NotNull final BiPredicate<String, String> matcher) {
            this.matcher = matcher;
        }

        final boolean appliesTo(@NotNull final String name, @NotNull final String search) {
            return this.matcher.test(name, search);
        }
    }

    private enum ThreadPriority {
        IDLE, // java prio 1, os prio (nice level) 4
        LOWEST, // java prio 2, os prio (nice level) 3
        VERY_LOW, // java prio 3, os prio (nice level) 2
        LOW, // java prio 4, os prio (nice level) 1
        NORMAL, // java prio 5, os prio (nice level) 0
        ABOVE_NORMAL, // java prio 6, os prio (nice level) -1
        HIGH, // java prio 7, os prio (nice level) -2
        VERY_HIGH, // java prio 8, os prio (nice level) -3
        HIGHEST, // java prio 9, os prio (nice level) -4
        CRITICAL; // java prio 10, os prio (nice level) -5

        private final int getPriority() {
            return this.ordinal() + 1;
        }
    }

    private record ThreadPriorityTweak(@NotNull String threadName,
                                       @NotNull ThreadPriorityTweaker.NameMatcherMode nameMatcherMode,
                                       @NotNull ThreadPriorityTweaker.ThreadPriority priority) {
        private final boolean appliesTo(@NotNull final String threadName) {
            return this.nameMatcherMode.appliesTo(threadName, this.threadName);
        }

        private final void applyTo(@NotNull final Thread thread) {
            ThreadPriorityTweaker.tweakPriority(thread, this.priority.getPriority());
        }
    }
}
