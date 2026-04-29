package gg.darkutils;

import gg.darkutils.utils.MathUtils;
import gg.darkutils.utils.RoundingMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class MathUtilsTest {
    @Test
    final void isNearEqual_withinEpsilon_returnsTrue() {
        Assertions.assertTrue(MathUtils.isNearEqual(1.0D, 1.000_000_000_01), "Values within epsilon should be considered equal");
    }

    @Test
    final void isNearEqual_outsideEpsilon_returnsFalse() {
        Assertions.assertFalse(MathUtils.isNearEqual(1.0D, 1.000_000_01), "Values outside epsilon should not be considered equal");
    }

    @Test
    final void round_up() {
        Assertions.assertEquals(2, MathUtils.round(1.4D, RoundingMode.UP), "UP rounding should round 1.4 to 2");
        Assertions.assertEquals(2, MathUtils.round(1.5D, RoundingMode.UP), "UP rounding should round 1.5 to 2");
    }

    @Test
    final void round_down() {
        Assertions.assertEquals(1, MathUtils.round(1.6D, RoundingMode.DOWN), "DOWN rounding should round 1.6 to 1");
    }

    @Test
    final void round_halfUp() {
        Assertions.assertEquals(2, MathUtils.round(1.5D, RoundingMode.HALF_UP), "HALF_UP should round 1.5 to 2");
    }

    @Test
    final void round_halfEven() {
        Assertions.assertEquals(2, MathUtils.round(2.5D, RoundingMode.HALF_EVEN), "HALF_EVEN should round 2.5 to 2");
        Assertions.assertEquals(4, MathUtils.round(3.5D, RoundingMode.HALF_EVEN), "HALF_EVEN should round 3.5 to 4");
    }

    @Test
    final void round_halfDown_exactTie_goesDown() {
        Assertions.assertEquals(1, MathUtils.round(1.5D, RoundingMode.HALF_DOWN), "HALF_DOWN should round exact tie down");
    }

    @Test
    final void round_halfDown_aboveTie_goesUp() {
        Assertions.assertEquals(2, MathUtils.round(1.500_000_1D, RoundingMode.HALF_DOWN), "HALF_DOWN should round above tie up");
    }
}