package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import org.junit.jupiter.api.Test;

/**
 * The top of the flagpole is actually reachable.
 *
 * <p>FlagPoleBlock has always paid by grab height across eight bands and given a 1-Up for the top
 * one. The finish apron was flat ground, so from a standing run the player could reach the bottom
 * two or three — six bands and the 1-Up were dead code in every course ever generated. Nothing was
 * broken enough to fail a test: the pole scored correctly, for heights nobody could get to.
 *
 * <p>So these tests are about the <em>geometry that makes the mechanic usable</em>, which is the
 * part no unit test was looking at. A green build proved the scoring worked; only the shape of the
 * ground in front of the pole decides whether it can ever be exercised.
 */
class FinishStaircaseTest {

    private static final int LENGTH = 360;

    /** How high the top of the pole sits above the finish floor, per the composer. */
    private static final int POLE_TOP = 8;

    @Test
    void everyCourseEndsOnAStaircaseThatReachesJumpingRangeOfTheTop() {
        for (CourseTheme theme : CourseTheme.values()) {
            for (long seed = 0; seed < 12; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, LENGTH, 3, seed);
                int flagX = c.flagX();
                int floorY = floorAtFlag(c, flagX);

                int highest = Integer.MIN_VALUE;
                for (int x = flagX - 12; x < flagX; x++) {
                    highest = Math.max(highest, topSolid(c, x, floorY));
                }
                // One below the top of the pole. Standing on the top step must not be enough by
                // itself — if it were, the 1-Up would be a toll rather than a jump.
                assertEquals(floorY + POLE_TOP - 1, highest,
                        theme + "/" + seed + ": top step is not one below the top of the pole");
            }
        }
    }

    @Test
    void theStaircaseIsClimbableOneBlockAtATime() {
        CourseComposer.Composition c = CourseComposer.compose(CourseTheme.GRASS, LENGTH, 3, 7L);
        int flagX = c.flagX();
        int floorY = floorAtFlag(c, flagX);

        int previous = topSolid(c, flagX - 12, floorY);
        for (int x = flagX - 11; x < flagX - 3; x++) {
            int here = topSolid(c, x, floorY);
            // A staircase with a two-block riser is a wall, and the player has to jump the whole
            // way up it rather than run up it — which is exactly the flat-apron problem again.
            assertTrue(here - previous <= 1,
                    "riser of " + (here - previous) + " at x=" + x);
            previous = here;
        }
    }

    @Test
    void thereIsStillFlatGroundUnderThePole() {
        // The staircase must not become the only way to the flag. A player who ignores it walks
        // along the floor and finishes at the bottom band, and CourseReachability's proof that the
        // flag is reachable depends on that floor still being there.
        CourseComposer.Composition c = CourseComposer.compose(CourseTheme.LAVA, LENGTH, 3, 3L);
        int flagX = c.flagX();
        int floorY = floorAtFlag(c, flagX);
        for (int x = flagX - 3; x < flagX; x++) {
            assertEquals(floorY, topSolid(c, x, floorY),
                    "x=" + x + " is not clear floor in front of the pole");
        }
    }

    /** The finish floor, recovered from the pole: its base sits one block above the ground. */
    private static int floorAtFlag(CourseComposer.Composition c, int flagX) {
        return lowestPoleY(c, flagX) - 1;
    }

    private static int lowestPoleY(CourseComposer.Composition c, int flagX) {
        int lowest = Integer.MAX_VALUE;
        for (java.util.Map.Entry<Long, net.minecraft.world.level.block.state.BlockState> e
                : c.canvas().blocks().entrySet()) {
            long k = e.getKey();
            if (CourseCanvas.unpackX(k) == flagX && CourseCanvas.unpackZ(k) == 0
                    && e.getValue().getBlock()
                        instanceof com.studio.planeshift.common.block.FlagPoleBlock) {
                lowest = Math.min(lowest, CourseCanvas.unpackY(k));
            }
        }
        return lowest;
    }

    /** Height of the highest solid block in a column, or the floor if the column is bare. */
    private static int topSolid(CourseComposer.Composition c, int x, int floorY) {
        int top = floorY;
        for (java.util.Map.Entry<Long, net.minecraft.world.level.block.state.BlockState> e
                : c.canvas().blocks().entrySet()) {
            long k = e.getKey();
            if (CourseCanvas.unpackX(k) == x && CourseCanvas.unpackZ(k) == 0
                    && !e.getValue().isAir()) {
                top = Math.max(top, CourseCanvas.unpackY(k));
            }
        }
        return top;
    }
}
