package gg.darkutils;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventPriority;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.NonCancellableEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

final class EventSystemTest {
    @BeforeEach
    final void resetRegistry() {
        EventRegistry.centralRegistry().getEventHandler(EventSystemTest.TestEvent.class).clearListeners();
        EventRegistry.centralRegistry().getEventHandler(EventSystemTest.TestCancellableEvent.class).clearListeners();
    }

    @Test
    final void testListenerInvocation() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((EventSystemTest.TestEvent e) -> counter.incrementAndGet());

        new EventSystemTest.TestEvent(1).trigger();

        Assertions.assertEquals(1, counter.get(), "Listener should be invoked once");
    }

    @Test
    final void testPriorityOrdering() {
        final var registry = EventRegistry.centralRegistry();

        final var order = new ArrayList<Integer>(3);

        registry.addListener((EventSystemTest.TestEvent e) -> order.add(3), EventPriority.LOW);
        registry.addListener((EventSystemTest.TestEvent e) -> order.add(1), EventPriority.HIGHEST);
        registry.addListener((EventSystemTest.TestEvent e) -> order.add(2), EventPriority.NORMAL);

        new EventSystemTest.TestEvent(0).trigger();

        Assertions.assertEquals(List.of(1, 2, 3), order, "Listeners should execute in priority order");
    }

    @Test
    final void testStableRegistrationOrder() {
        final var registry = EventRegistry.centralRegistry();

        final var order = new ArrayList<Integer>(3);

        registry.addListener((EventSystemTest.TestEvent e) -> order.add(1), EventPriority.NORMAL);
        registry.addListener((EventSystemTest.TestEvent e) -> order.add(2), EventPriority.NORMAL);
        registry.addListener((EventSystemTest.TestEvent e) -> order.add(3), EventPriority.NORMAL);

        new EventSystemTest.TestEvent(0).trigger();

        Assertions.assertEquals(List.of(1, 2, 3), order, "Listeners with same priority should preserve registration order");
    }

    @Test
    final void testCancellationStopsListeners() {
        final var registry = EventRegistry.centralRegistry();

        registry.addListener((EventSystemTest.TestCancellableEvent e) -> e.cancellationState().cancel());
        final var counter = new AtomicInteger(0);
        registry.addListener((EventSystemTest.TestCancellableEvent e) -> counter.incrementAndGet());

        new EventSystemTest.TestCancellableEvent().trigger();

        Assertions.assertEquals(0, counter.get(), "Cancelled event should stop downstream listeners");
    }

    @Test
    final void testReceiveCancelled() {
        final var registry = EventRegistry.centralRegistry();

        registry.addListener((EventSystemTest.TestCancellableEvent e) -> e.cancellationState().cancel());
        final var counter = new AtomicInteger(0);
        registry.addListener((EventSystemTest.TestCancellableEvent e) -> counter.incrementAndGet(),
                EventPriority.NORMAL,
                true);

        new EventSystemTest.TestCancellableEvent().trigger();

        Assertions.assertEquals(1, counter.get(), "Listener with receiveCancelled=true should be invoked");
    }

    @Test
    final void testUncancelRestoresDownstreamPropagation() {
        final var registry = EventRegistry.centralRegistry();

        registry.addListener((EventSystemTest.TestCancellableEvent e) -> e.cancellationState().cancel(),
                EventPriority.HIGHEST);

        registry.addListener((EventSystemTest.TestCancellableEvent e) -> e.cancellationState().uncancel(),
                EventPriority.ABOVE_NORMAL,
                true);

        final var counter = new AtomicInteger(0);
        registry.addListener((EventSystemTest.TestCancellableEvent e) -> counter.incrementAndGet(),
                EventPriority.NORMAL,
                false);

        new EventSystemTest.TestCancellableEvent().trigger();

        Assertions.assertEquals(1, counter.get(), "Uncancel should allow downstream listeners to run");
    }

    @Test
    final void testTriggerAndNotCancelledContract() {
        final var registry = EventRegistry.centralRegistry();

        registry.addListener((EventSystemTest.TestCancellableEvent e) -> e.cancellationState().cancel());

        final var result = new EventSystemTest.TestCancellableEvent().triggerAndNotCancelled();

        Assertions.assertFalse(result, "triggerAndNotCancelled should return false when cancelled");
    }

    @Test
    final void testReentrancy() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((final EventSystemTest.TestEvent e) -> {
            if (3 > e.value()) {
                counter.incrementAndGet();
                new EventSystemTest.TestEvent(e.value() + 1).trigger();
            }
        });

        new EventSystemTest.TestEvent(0).trigger();

        Assertions.assertEquals(3, counter.get(), "Reentrant dispatch should increment counter 3 times");
    }

    @Test
    final void testNestedDispatchIsolation() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((final EventSystemTest.TestEvent e) -> {
            new EventSystemTest.TestCancellableEvent().trigger();
            counter.incrementAndGet();
        });

        registry.addListener((EventSystemTest.TestCancellableEvent e) ->
                e.cancellationState().cancel());

        new EventSystemTest.TestEvent(0).trigger();

        Assertions.assertEquals(1, counter.get(), "Nested dispatch should not interfere with outer event");
    }

    @Test
    final void testThreadSafety() throws InterruptedException {
        final var registry = EventRegistry.centralRegistry();

        final var threads = Runtime.getRuntime().availableProcessors();

        final var counter = new AtomicInteger(0);

        registry.addListener((EventSystemTest.TestEvent e) -> counter.incrementAndGet());

        final var latch = new CountDownLatch(threads);
        final var iterations = 10_000;
        final Runnable task = () -> {
            for (var j = 0; iterations > j; ++j) {
                new EventSystemTest.TestEvent(0).trigger();
            }
            latch.countDown();
        };

        for (var i = 0; threads > i; ++i) {
            Thread.startVirtualThread(task);
        }

        latch.await();

        Assertions.assertEquals(threads * iterations, counter.get(), "All threads should increment counter correctly");
    }

    private record TestEvent(int value) implements NonCancellableEvent {
    }

    private record TestCancellableEvent(@NotNull CancellationState cancellationState)
            implements CancellableEvent {
        private TestCancellableEvent() {
            this(CancellationState.ofFresh());
        }
    }
}