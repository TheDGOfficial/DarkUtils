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
                yield MathUtils.EPSILON > Math.abs(frac - 0.5D) ? Math.floor(number) : Math.round(number);
            }
        };
    }
}
