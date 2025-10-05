package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an immutable pair of two values.
 * <p>
 * Records have minimal overhead and is the best pick for a Pair class.
 * This is usually more performant than using a Pair class while also being shorter and cleaner.
 * <p>
 * Note that this still not the most performant solution if the types are wrapper classes of primitives.
 * Use the primitive variants, e.g. {@link Pair.Int}
 *
 * @param first  The first value.
 * @param second The second value.
 * @param <A>    The type of the first value.
 * @param <B>    The type of the second value.
 */
public record Pair<A, B>(@NotNull A first, @NotNull B second) {
    /**
     * Represents an immutable pair of two ints.
     * <p>
     * See {@link Pair}.
     *
     * @param first  The first int.
     * @param second The second int.
     */
    private record Int(int first, int second) {
    }
}
