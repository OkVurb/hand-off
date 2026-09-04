package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The flag pole payout curve.
 *
 * <p>Pure arithmetic, so it is worth pinning: the lookup is indexed by a band computed from block
 * positions at runtime, and an off-by-one there is invisible in game — every grab still pays
 * <em>something</em>, just the wrong something.
 */
class FlagpoleScoreTest {

    @Test
    void everyBandPaysMoreThanTheOneBelowIt() {
        for (int band = 1; band < CourseScoringService.FLAGPOLE_LADDER.length; band++) {
            assertTrue(CourseScoringService.flagpoleValue(band)
                            > CourseScoringService.flagpoleValue(band - 1),
                    "band " + band + " must beat band " + (band - 1));
        }
    }

    @Test
    void outOfRangeBandsClampInsteadOfThrowing() {
        // A player-built pole can be any height, and heightBand walks real blocks.
        assertEquals(CourseScoringService.FLAGPOLE_LADDER[0], CourseScoringService.flagpoleValue(-3));
        assertEquals(CourseScoringService.FLAGPOLE_LADDER[0], CourseScoringService.flagpoleValue(0));
        int top = CourseScoringService.FLAGPOLE_LADDER[CourseScoringService.FLAGPOLE_LADDER.length - 1];
        assertEquals(top, CourseScoringService.flagpoleValue(999));
    }

    @Test
    void onlyTheTopBandPaysALife() {
        int last = CourseScoringService.FLAGPOLE_LADDER.length - 1;
        assertTrue(CourseScoringService.isTopBand(last));
        assertTrue(CourseScoringService.isTopBand(last + 5), "an over-tall pole still tops out");
        for (int band = 0; band < last; band++) {
            assertFalse(CourseScoringService.isTopBand(band), "band " + band + " must not pay a life");
        }
    }

    @Test
    void theTopIsWorthGoingOutOfYourWayFor() {
        // The whole point of the feature: if the top is not dramatically better than the base,
        // nobody aims for it and the pole stays decoration.
        int base = CourseScoringService.flagpoleValue(0);
        int top = CourseScoringService.flagpoleValue(CourseScoringService.FLAGPOLE_LADDER.length - 1);
        assertTrue(top >= base * 20, "top band was " + top + " against a base of " + base);
    }
}
