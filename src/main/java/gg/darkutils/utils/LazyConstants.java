package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.AbstractMap;
import java.util.Set;
import java.util.Collections;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

// TODO Remove the --enable-preview when the JEP lands in a Stable JDK version
// TODO Remove the fallbacks entirely when the JEP lands in a LTS JDK version
public final class LazyConstants {
    private LazyConstants() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    @NotNull
    public static final <K, V> Map<K, V> lazyMapOf(@NotNull final Set<@NotNull K> allPossibleKeys, @NotNull final Function<? super K, ? extends V> valueMapper) {
        // J26 EA preview
        // return Map.ofLazy(allPossibleKeys, valueMapper);

        // J25 preview
        // return StableValue.map(allPossibleKeys, valueMapper);

        // non-preview
        Objects.requireNonNull(allPossibleKeys, "allPossibleKeys");
        Objects.requireNonNull(valueMapper, "valueMapper");

        final var lazyValues = new ConcurrentHashMap<K, Supplier<V>>(allPossibleKeys.size());

        for (final var key : allPossibleKeys) {
            Objects.requireNonNull(key, "key");

            lazyValues.put(key, LazyConstants.lazyConstantOf(() -> valueMapper.apply(key)));
        }

        return Collections.unmodifiableMap(new AbstractMap<>() {
            @Override
            @NotNull
            public final V get(@NotNull final Object key) {
                Objects.requireNonNull(key, "key");

                final var lazyValue = Objects.requireNonNull(
                        lazyValues.get(key),
                        "Unknown key passed to lazy map: " + key
                );

                return Objects.requireNonNull(
                        lazyValue.get(),
                        "Lazy value supplier returned null for key: " + key
                );
            }

            @Override
            @NotNull
            public final V getOrDefault(@NotNull final Object key, @NotNull final V def) {
                throw new UnsupportedOperationException(
                        "getOrDefault over lazy map is not supported, use get instead"
                );
            }

            @Override
            @NotNull
            public final Set<Map.Entry<K, V>> entrySet() {
                throw new UnsupportedOperationException(
                        "Iteration over lazy map is not supported"
                );
            }
        });
    }

    @NotNull
    public static final <T> Supplier<T> lazyConstantOf(@NotNull final Supplier<? extends T> supplier) {
        // J26 EA preview
        // return LazyConstant.of(supplier);

        // J25 preview
        // return StableValue.supplier(supplier);

        // non-preview
        return Suppliers.memoize(supplier::get);
    }
}

