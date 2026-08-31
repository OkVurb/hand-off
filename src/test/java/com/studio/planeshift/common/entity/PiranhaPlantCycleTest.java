package com.studio.planeshift.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the Piranha Plant emerge curve.
 *
 * <p>The cycle is what makes the plant a rhythm puzzle rather than an unfair hazard, so the
 * shape of it matters: it has to be fully hidden for a stretch, fully exposed for a stretch, and
 * move continuously between the two with no jumps a player could not read.
 */
class PiranhaPlantCycleTest {

    private static final float EPSILON = 1.0E-4F;

    @Test
    @DisplayName("the cycle starts fully hidden")
    void startsHidden() {
        assertEquals(0.0F, PiranhaPlantEntity.extensionFor(0), EPSILON);
    }

    @Test
    @DisplayName("the curve stays within 0 and 1 across a whole cycle")
    void staysInRange() {
        for (int t = 0; t < 400; t++) {
            float e = PiranhaPlantEntity.extensionFor(t);
            assertTrue(e >= 0.0F && e <= 1.0F, "extension out of range at tick " + t + ": " + e);
        }
    }

    @Test
    @DisplayName("the plant reaches fully out and fully hidden at some point")
    void reachesBothExtremes() {
        boolean sawHidden = false;
        boolean sawExposed = false;
        for (int t = 0; t < 400; t++) {
            float e = PiranhaPlantEntity.extensionFor(t);
            if (e <= EPSILON) {
                sawHidden = true;
            }
            if (e >= 1.0F - EPSILON) {
                sawExposed = true;
            }
        }
        assertTrue(sawHidden, "the plant must fully retract, or it can never be passed safely");
        assertTrue(sawExposed, "the plant must fully emerge, or it is never a threat");
    }

    @Test
    @DisplayName("the curve is continuous: no tick jumps more than one step")
    void isContinuous() {
        // A discontinuity would teleport the plant through the player rather than biting them,
        // which reads as the hitbox being broken.
        float maxStep = 1.0F / 20.0F + EPSILON;
        for (int t = 1; t < 400; t++) {
            float delta = Math.abs(PiranhaPlantEntity.extensionFor(t)
                    - PiranhaPlantEntity.extensionFor(t - 1));
            assertTrue(delta <= maxStep,
                    "jump of " + delta + " at tick " + t + " exceeds one animation step");
        }
    }

    @Test
    @DisplayName("the cycle repeats exactly")
    void repeats() {
        for (int t = 0; t < 60; t++) {
            assertEquals(PiranhaPlantEntity.extensionFor(t),
                    PiranhaPlantEntity.extensionFor(t + 130), EPSILON,
                    "tick " + t + " should match one full cycle later");
        }
    }

    @Test
    @DisplayName("negative ticks wrap rather than throwing")
    void handlesNegativeTicks() {
        // floorMod, not %, so a clock that goes negative does not produce a negative extension
        // and drive the plant below its pipe.
        assertTrue(PiranhaPlantEntity.extensionFor(-1) >= 0.0F);
        assertTrue(PiranhaPlantEntity.extensionFor(-500) >= 0.0F);
    }
}
