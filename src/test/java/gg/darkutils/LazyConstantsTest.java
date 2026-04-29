package gg.darkutils;

import gg.darkutils.utils.LazyConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

final class LazyConstantsTest {
    @Test
    final void lazyConstantOf_computesOnlyOnce() {
        final var counter = new AtomicInteger(0);

        final var supplier = LazyConstants.lazyConstantOf(counter::incrementAndGet);

        final var first = supplier.get();
        final var second = supplier.get();
        final var third = supplier.get();

        Assertions.assertEquals(1, first, "First computation should return 1");
        Assertions.assertEquals(first, second, "Second call should return cached value");
        Assertions.assertEquals(second, third, "Third call should return cached value");
        Assertions.assertEquals(1, counter.get(), "Supplier should be invoked only once");
    }

    @Test
    final void lazyMapOf_valueMapperCalledOnlyOncePerKey() {
        final var counter = new AtomicInteger(0);

        final var map =
                LazyConstants.lazyMapOf(Set.of(1, 2), key -> counter.incrementAndGet());

        final var first = map.get(1);
        final var second = map.get(1);

        Assertions.assertEquals(first, second, "Same key should return same cached value");
        Assertions.assertEquals(1, counter.get(), "Value mapper should be called only once per key");
    }
}