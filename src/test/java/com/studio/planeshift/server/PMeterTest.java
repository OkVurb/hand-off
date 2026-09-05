package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The P-meter curve.
 *
 * <p>Fill and drain rates are the kind of number that can be wrong by a factor of two for weeks
 * without anyone being able to point at what is off — the meter still fills, it just feels bad.
 * These tests say how long each phase takes, so changing one is a decision rather than a drift.
 */
class PMeterTest {

    /** Runs the meter for n ticks and returns the final charge. */
    private static int run(int charge, int hold, boolean driving, int ticks) {
        long packed = ((long) charge << 32) | hold;
        for (int i = 0; i < ticks; i++) {
            packed = PMeter.advance(PMeter.charge(packed), PMeter.hold(packed), driving);
        }
        return PMeter.charge(packed);
    }

    @Test
    void fillsInExactlyFillTicks() {
        assertFalse(PMeter.atFull(run(0, 0, true, PMeter.FILL_TICKS - 1)));
        assertTrue(PMeter.atFull(run(0, 0, true, PMeter.FILL_TICKS)));
    }

    @Test
    void theHoldProtectsAFullMeterForABriefWindow() {
        // The point of the whole mechanic: a jump or a turn must not cost the run.
        assertTrue(PMeter.atFull(run(PMeter.FILL_TICKS, PMeter.HOLD_TICKS, false, PMeter.HOLD_TICKS)),
                "still full at the end of the hold");
        assertFalse(PMeter.atFull(run(PMeter.FILL_TICKS, PMeter.HOLD_TICKS, false,
                        PMeter.HOLD_TICKS + PMeter.DRAIN_TICKS + 2)),
                "and empty a full drain later");
    }

    @Test
    void drivingRefreshesTheHold() {
        long packed = PMeter.advance(10, 0, true);
        assertEquals(PMeter.HOLD_TICKS, PMeter.hold(packed));
    }

    @Test
    void aFullMeterEmptiesInExactlyDrainTicks() {
        // This is the test that caught the original implementation: the drain was written as
        // FILL_TICKS / DRAIN_TICKS per tick, which is integer division, so the constant was
        // decorative and the real drain time was always FILL_TICKS.
        assertEquals(0, run(PMeter.FILL_TICKS, 0, false, PMeter.DRAIN_TICKS));
        assertFalse(run(PMeter.FILL_TICKS, 0, false, PMeter.DRAIN_TICKS - 1) == 0,
                "and not before");
    }

    @Test
    void theHoldIsWhatMakesTheMechanicForgiving() {
        // Not an asymmetric drain, which would need a sub-tick accumulator. The hold protects the
        // entire meter through a jump or a turn; the drain is what happens once you have genuinely
        // stopped, and that has no reason to be gentle.
        assertTrue(PMeter.HOLD_TICKS > 0);
        assertEquals(PMeter.FILL_TICKS, PMeter.DRAIN_TICKS);
    }

    @Test
    void theDisplayStepIsClampedAndMonotonic() {
        assertEquals(0, PMeter.step(0));
        assertEquals(PMeter.STEPS, PMeter.step(PMeter.FILL_TICKS));
        assertEquals(PMeter.STEPS, PMeter.step(PMeter.FILL_TICKS * 5), "clamped above full");
        int previous = -1;
        for (int charge = 0; charge <= PMeter.FILL_TICKS; charge++) {
            int step = PMeter.step(charge);
            assertTrue(step >= previous, "step went backwards at charge " + charge);
            assertTrue(step <= PMeter.STEPS);
            previous = step;
        }
    }

    @Test
    void aPartialMeterShowsAPartialBar() {
        // Guards the off-by-one that would make the bar read empty until it is suddenly full.
        int half = PMeter.step(PMeter.FILL_TICKS / 2);
        assertTrue(half > 0 && half < PMeter.STEPS, "half a meter drew " + half + " segments");
    }
}
