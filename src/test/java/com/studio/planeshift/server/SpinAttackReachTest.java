package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The spin sweep's reach.
 *
 * <p>Pinned for the same reason the ground pound's is: a reach that is slightly too generous still
 * looks like a working move, and quietly makes the stomp optional. Every enemy placement in
 * SegmentLibrary assumes the player has to commit to a jump.
 */
class SpinAttackReachTest {

    @Test
    void thingsBesideYouAreSwept() {
        assertTrue(SpinAttackService.inReach(0.0D, 0.0D, 0.0D));
        assertTrue(SpinAttackService.inReach(SpinAttackService.REACH - 0.1D, 0.0D, 0.0D));
        assertTrue(SpinAttackService.inReach(0.0D, 0.0D, -(SpinAttackService.REACH - 0.1D)));
    }

    @Test
    void reachIsACircleNotASquare() {
        double r = SpinAttackService.REACH;
        assertFalse(SpinAttackService.inReach(r + 0.1D, 0.0D, 0.0D));
        // Diagonal at 0.8r per axis is 1.13r away, so it must miss. Measuring with
        // max(|dx|,|dz|) would include it and hand the spin 41% more reach at the corners.
        assertFalse(SpinAttackService.inReach(r * 0.8D, 0.0D, r * 0.8D));
    }

    @Test
    void itIsAWaistHeightSweepNotAColumn() {
        double v = SpinAttackService.VERTICAL_REACH;
        assertTrue(SpinAttackService.inReach(0.0D, v - 0.1D, 0.0D));
        assertFalse(SpinAttackService.inReach(0.0D, v + 0.1D, 0.0D),
                "an enemy standing on your head is not beside you");
        assertFalse(SpinAttackService.inReach(0.0D, -(v + 0.1D), 0.0D));
    }

    @Test
    void theSpinDoesNotOutrangeTheGroundPound() {
        // The pound costs a whole airborne stint and lands you in one place; the spin is free and
        // happens on the ground. If the free move reached further there would be no reason to pound.
        assertTrue(SpinAttackService.REACH < GroundPoundResolver.STAGGER_RADIUS);
    }
}
