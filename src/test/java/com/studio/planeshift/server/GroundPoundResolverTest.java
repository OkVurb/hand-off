package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.studio.planeshift.server.GroundPoundResolver.Response;
import org.junit.jupiter.api.Test;

/**
 * The ground pound shockwave's targeting rules.
 *
 * <p>Worth pinning because every one of these is invisible in a build log and awkward to see in
 * game: a wave that reaches one block too far, or that catches an enemy on the platform above,
 * still looks like a working move — it just quietly stops being a move about aim.
 */
class GroundPoundResolverTest {

    private static Response at(double dx, double dz, boolean shelled) {
        return GroundPoundResolver.classify(dx, 0.0D, dz, shelled, true, false);
    }

    @Test
    void shelledEnemiesInsideTheInnerRingAreFlipped() {
        assertEquals(Response.FLIP, at(0.0D, 0.0D, true));
        assertEquals(Response.FLIP, at(GroundPoundResolver.FLIP_RADIUS - 0.1D, 0.0D, true));
    }

    @Test
    void aShelledEnemyPastTheInnerRingIsOnlyStaggered() {
        // The flip has to be aimed. If it reached as far as the stagger, the move would stop being
        // "land on the Buzzy Beetle" and become "land somewhere near it".
        assertEquals(Response.STAGGER, at(GroundPoundResolver.FLIP_RADIUS + 0.1D, 0.0D, true));
    }

    @Test
    void unshelledEnemiesAreNeverFlipped() {
        assertEquals(Response.STAGGER, at(0.0D, 0.0D, false));
    }

    @Test
    void rangeIsHorizontalAndMeasuredAsACircle() {
        double r = GroundPoundResolver.STAGGER_RADIUS;
        assertEquals(Response.STAGGER, at(r - 0.05D, 0.0D, false));
        assertEquals(Response.NONE, at(r + 0.05D, 0.0D, false));
        // A diagonal at the same per-axis distance must fall outside: using max(|dx|,|dz|) instead
        // of the true distance would make the wave a square, reaching ~41% further at the corners.
        assertEquals(Response.NONE, at(r * 0.8D, r * 0.8D, false));
    }

    @Test
    void theWaveDoesNotClimb() {
        double far = GroundPoundResolver.VERTICAL_REACH + 0.5D;
        assertEquals(Response.NONE,
                GroundPoundResolver.classify(0.0D, far, 0.0D, true, true, false),
                "an enemy on the platform above is not standing in the wave");
        assertEquals(Response.NONE,
                GroundPoundResolver.classify(0.0D, -far, 0.0D, true, true, false));
    }

    @Test
    void airborneAndImmuneEnemiesAreSkippedBeforeRangeIsEvenConsidered() {
        assertEquals(Response.NONE,
                GroundPoundResolver.classify(0.0D, 0.0D, 0.0D, true, false, false),
                "no feet to sweep");
        assertEquals(Response.NONE,
                GroundPoundResolver.classify(0.0D, 0.0D, 0.0D, true, true, true),
                "opted out");
    }
}
