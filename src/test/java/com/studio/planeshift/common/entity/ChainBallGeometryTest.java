package com.studio.planeshift.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The drawn arc has to be the arc the ball actually swings through.
 *
 * <p>Two angle conventions meet in this entity: the chain measures from straight down, and
 * {@code Telegraph.arc} measures anticlockwise from east. A quarter turn between them draws the
 * warning somewhere the ball never goes, and that is strictly worse than drawing nothing -- a
 * telegraph the player learns to trust and which then lies is how a fair hazard becomes an unfair
 * one.
 *
 * <p>Nothing here needs a Level, which is the point of the maths being static.
 */
class ChainBallGeometryTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("a slack chain hangs straight down")
    void hangsDown() {
        Vec3 at = ChainBallEntity.ballOffset(0.0D, 4.0D);
        assertEquals(0.0D, at.x, EPS);
        assertEquals(-4.0D, at.y, EPS, "zero degrees should be straight down, not sideways");
    }

    @Test
    @DisplayName("the ball never leaves the end of its chain")
    void staysOnTheChain() {
        for (double a = -170.0D; a <= 170.0D; a += 1.0D) {
            Vec3 at = ChainBallEntity.ballOffset(a, 4.0D);
            assertEquals(4.0D, Math.hypot(at.x, at.y), 1e-9,
                    "chain stretched or slackened at " + a + " degrees");
        }
    }

    @Test
    @DisplayName("the arc endpoints land exactly on the ball at its swing extremes")
    void arcMatchesTheSwing() {
        double length = 5.0D;
        for (double sweep : new double[] {15.0D, 45.0D, 60.0D, 120.0D}) {
            for (double end : new double[] {-sweep, sweep}) {
                Vec3 ball = ChainBallEntity.ballOffset(end, length);
                // Where Telegraph.arc would put that same endpoint.
                double std = Math.toRadians(ChainBallEntity.standardAngle(end));
                Vec3 drawn = new Vec3(Math.cos(std) * length, Math.sin(std) * length, 0.0D);
                assertTrue(ball.distanceTo(drawn) < 1e-9,
                        "at sweep " + sweep + " the arc endpoint " + drawn
                                + " is not where the ball is, " + ball
                                + " -- the two angle conventions disagree");
            }
        }
    }
}
