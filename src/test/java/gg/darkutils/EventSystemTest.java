package gg.darkutils;

import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventPriority;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.EventListener;
import gg.darkutils.events.base.NonCancellableEvent;

import org.jetbrains.annotations.NotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

final class EventSystemTest {
    record TestEvent(int value) implements NonCancellableEvent {}

    record TestCancellableEvent(@NotNull CancellationState cancellationState)
            implements CancellableEvent {

        TestCancellableEvent() {
            this(CancellationState.ofFresh());
        }
    }

    @BeforeEach
    void resetRegistry() {
        clearListeners(TestEvent.class);
        clearListeners(TestCancellableEvent.class);
    }

    @SuppressWarnings("unchecked")
    <T extends Event> void clearListeners(Class<T> event) {
        final var handler = EventRegistry.centralRegistry().getEventHandler(event);

        handler.getListeners().forEach((l) -> handler.removeListener((EventListener<T>) l));
    }

    @Test
    void testListenerInvocation() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((TestEvent e) -> counter.incrementAndGet());

        new TestEvent(1).trigger();

        Assertions.assertEquals(1, counter.get());
    }

    @Test
    void testPriorityOrdering() {
        final var registry = EventRegistry.centralRegistry();

        final var order = new ArrayList<Integer>();

        registry.addListener((TestEvent e) -> order.add(3), EventPriority.LOW);
        registry.addListener((TestEvent e) -> order.add(1), EventPriority.HIGHEST);
        registry.addListener((TestEvent e) -> order.add(2), EventPriority.NORMAL);

        new TestEvent(0).trigger();

        Assertions.assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    void testStableRegistrationOrder() {
        final var registry = EventRegistry.centralRegistry();

        final var order = new ArrayList<Integer>();

        registry.addListener((TestEvent e) -> order.add(1), EventPriority.NORMAL);
        registry.addListener((TestEvent e) -> order.add(2), EventPriority.NORMAL);
        registry.addListener((TestEvent e) -> order.add(3), EventPriority.NORMAL);

        new TestEvent(0).trigger();

        Assertions.assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    void testCancellationStopsListeners() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((TestCancellableEvent e) -> e.cancellationState().cancel());
        registry.addListener((TestCancellableEvent e) -> counter.incrementAndGet());

        new TestCancellableEvent().trigger();

        Assertions.assertEquals(0, counter.get());
    }

    @Test
    void testReceiveCancelled() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((TestCancellableEvent e) -> e.cancellationState().cancel());
        registry.addListener((TestCancellableEvent e) -> counter.incrementAndGet(),
                EventPriority.NORMAL,
                true);

        new TestCancellableEvent().trigger();

        Assertions.assertEquals(1, counter.get());
    }

    @Test
    void testUncancelRestoresDownstreamPropagation() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((TestCancellableEvent e) -> e.cancellationState().cancel(),
                EventPriority.HIGHEST);

        registry.addListener((TestCancellableEvent e) -> e.cancellationState().uncancel(),
                EventPriority.ABOVE_NORMAL,
                true);

        registry.addListener((TestCancellableEvent e) -> counter.incrementAndGet(),
                EventPriority.NORMAL,
                false);

        new TestCancellableEvent().trigger();

        Assertions.assertEquals(1, counter.get());
    }

    @Test
    void testTriggerAndNotCancelledContract() {
        final var registry = EventRegistry.centralRegistry();

        registry.addListener((TestCancellableEvent e) -> e.cancellationState().cancel());

        final var result = new TestCancellableEvent().triggerAndNotCancelled();

        Assertions.assertFalse(result);
    }

    @Test
    void testReentrancy() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((TestEvent e) -> {
            if (e.value() < 3) {
                counter.incrementAndGet();
                new TestEvent(e.value() + 1).trigger();
            }
        });

        new TestEvent(0).trigger();

        Assertions.assertEquals(3, counter.get());
    }

    @Test
    void testNestedDispatchIsolation() {
        final var registry = EventRegistry.centralRegistry();

        final var counter = new AtomicInteger(0);

        registry.addListener((TestEvent e) -> {
            new TestCancellableEvent().trigger();
            counter.incrementAndGet();
        });

        registry.addListener((TestCancellableEvent e) ->
                e.cancellationState().cancel());

        new TestEvent(0).trigger();

        Assertions.assertEquals(1, counter.get());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        final var registry = EventRegistry.centralRegistry();

        final int threads = 8;
        final int iterations = 10_000;

        final var counter = new AtomicInteger(0);
        final var latch = new CountDownLatch(threads);

        registry.addListener((TestEvent e) -> counter.incrementAndGet());

        for (var i = 0; i < threads; i++) {
            new Thread(() -> {
                for (var j = 0; j < iterations; j++) {
                    new TestEvent(0).trigger();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        Assertions.assertEquals(threads * iterations, counter.get());
    }
}

