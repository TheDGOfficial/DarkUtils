package gg.darkutils;

import gg.darkutils.utils.MathUtils;
import gg.darkutils.utils.RoundingMode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

final class MathUtilsTest {
    @Test
    void isNearEqual_withinEpsilon_returnsTrue() {
        Assertions.assertTrue(MathUtils.isNearEqual(1.0D, 1.0D + 1e-11D));
    }

    @Test
    void isNearEqual_outsideEpsilon_returnsFalse() {
        Assertions.assertFalse(MathUtils.isNearEqual(1.0D, 1.0D + 1e-8D));
    }

    @Test
    void round_up() {
        Assertions.assertEquals(2, MathUtils.round(1.4D, RoundingMode.UP));
        Assertions.assertEquals(2, MathUtils.round(1.5D, RoundingMode.UP));
    }

    @Test
    void round_down() {
        Assertions.assertEquals(1, MathUtils.round(1.6D, RoundingMode.DOWN));
    }

    @Test
    void round_halfUp() {
        Assertions.assertEquals(2, MathUtils.round(1.5D, RoundingMode.HALF_UP));
    }

    @Test
    void round_halfEven() {
        Assertions.assertEquals(2, MathUtils.round(2.5D, RoundingMode.HALF_EVEN));
        Assertions.assertEquals(4, MathUtils.round(3.5D, RoundingMode.HALF_EVEN));
    }

    @Test
    void round_halfDown_exactTie_goesDown() {
        Assertions.assertEquals(1, MathUtils.round(1.5D, RoundingMode.HALF_DOWN));
    }

    @Test
    void round_halfDown_aboveTie_goesUp() {
        Assertions.assertEquals(2, MathUtils.round(1.5000001D, RoundingMode.HALF_DOWN));
    }
}

