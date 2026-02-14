package gg.darkutils;

import gg.darkutils.utils.LazyConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.Map;

final class LazyConstantsTest {
    @Test
    void lazyConstantOf_computesOnlyOnce() {
        final var counter = new AtomicInteger(0);

        final Supplier<Integer> supplier = LazyConstants.lazyConstantOf(counter::incrementAndGet);

        final var first = supplier.get();
        final var second = supplier.get();
        final var third = supplier.get();

        Assertions.assertEquals(1, first);
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(second, third);
        Assertions.assertEquals(1, counter.get());
    }

    @Test
    void lazyMapOf_valueMapperCalledOnlyOncePerKey() {
        final var counter = new AtomicInteger(0);

        final Map<Integer, Integer> map =
                LazyConstants.lazyMapOf(Set.of(1, 2), key -> counter.incrementAndGet());

        final var first = map.get(1);
        final var second = map.get(1);

        Assertions.assertEquals(first, second);
        Assertions.assertEquals(1, counter.get());
    }
}

