package gg.darkutils.utils;

/**
 * Represents a {@link RoundingMode}.
 */
public enum RoundingMode {
    /**
     * Rounds up.
     * {@snippet :
     * var mode = RoundingMode.UP;
     * var result = MathUtils.round(1.4, mode); // 2
     * var result = MathUtils.round(1.5, mode); // 2
     * var result = MathUtils.round(1.6, mode); // 2
     *}
     */
    UP,
    /**
     * Rounds down.
     * {@snippet :
     * var mode = RoundingMode.DOWN;
     * var result = MathUtils.round(1.4, mode); // 1
     * var result = MathUtils.round(1.5, mode); // 1
     * var result = MathUtils.round(1.6, mode); // 1
     *}
     */
    DOWN,
    /**
     * Half rounds up.
     * {@snippet :
     * var mode = RoundingMode.HALF_UP;
     * var result = MathUtils.round(1.4, mode); // 1
     * var result = MathUtils.round(1.5, mode); // 2
     * var result = MathUtils.round(1.6, mode); // 2
     *}
     */
    HALF_UP,
    /**
     * Half rounds down.
     * {@snippet :
     * var mode = RoundingMode.HALF_DOWN;
     * var result = MathUtils.round(1.4, mode); // 1
     * var result = MathUtils.round(1.5, mode); // 1
     * var result = MathUtils.round(1.6, mode); // 2
     *}
     */
    HALF_DOWN,
    /**
     * Half rounds to even.
     * {@snippet :
     * var mode = RoundingMode.HALF_EVEN;
     * var result = MathUtils.round(1.4, mode); // 1
     * var result = MathUtils.round(1.5, mode); // 2
     * var result = MathUtils.round(1.6, mode); // 2
     *
     * // Special examples for HALF_EVEN
     * var result = MathUtils.round(2.5, mode); // 2
     * var result = MathUtils.round(3.5, mode); // 4
     *}
     */
    HALF_EVEN
}
