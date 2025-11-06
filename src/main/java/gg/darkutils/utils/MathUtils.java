package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Math utilities.
 */
public final class MathUtils {
    /**
     * Represents the epsilon.
     */
    private static final double EPSILON = 1.0e-10D;

    private MathUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    /**
     * Checks whether two doubles are approximately equal within {@link MathUtils#EPSILON}.
     *
     * @param first  The first double.
     * @param second The second double.
     * @return {@code true} if the two doubles are within {@link MathUtils#EPSILON} of each other, {@code false} otherwise.
     */
    public static final boolean isNearEqual(final double first, final double second) {
        return MathUtils.EPSILON > Math.abs(first - second);
    }

    /**
     * Rounds the given number using the {@link RoundingMode} specified.
     *
     * @param number The number.
     * @param mode   The {@link RoundingMode}.
     * @return The rounded number using the given {@link RoundingMode}.
     */
    public static final int round(final double number, @NotNull final RoundingMode mode) {
        return (int) switch (mode) {
            case UP -> Math.ceil(number);
            case DOWN -> Math.floor(number);
            case HALF_UP -> Math.round(number);
            case HALF_EVEN -> Math.rint(number);
            case HALF_DOWN -> {
                final var floor = Math.floor(number);
                final var frac = number - floor;
                yield MathUtils.isNearEqual(frac, 0.5D) ? Math.floor(number) : Math.round(number);
            }
        };
    }
}
