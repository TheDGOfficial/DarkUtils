package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an immutable pair of two values.
 * <p>
 * Records have minimal overhead and is the best pick for a Pair class.
 * This is usually more performant than using a Pair class while also being shorter and cleaner.
 * <p>
 * Note that this still not the most performant solution if the types are wrapper classes of primitives.
 * Create a record for the primitive variants you need in that case.
 *
 * @param first  The first value.
 * @param second The second value.
 * @param <A>    The type of the first value.
 * @param <B>    The type of the second value.
 */
public record Pair<A, B>(@NotNull A first, @NotNull B second) {
}
