package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Scenery is scenery: it fills the world in and never changes the game.
 *
 * <p>The guarantee is structural rather than checked case by case. Decoration writes only at
 * depths {@link CourseDecorator#NEAR_Z} and beyond, the reachability solver reads only the play
 * plane, and the two sets of columns are disjoint — so no amount of scenery can make a course
 * harder. These tests pin that relationship, because it is the entire reason the decorator is
 * allowed to be as free with the canvas as it is.
 */
class CourseDecoratorTest {

    @Test
    void decorationNeverReachesTheLane() {
        // The lane is +/- GenContext.LANE_HALF_WIDTH around z=0, and scenery starts beyond it with
        // a clear column in between: flush against the play plane, scenery reads as level geometry
        // and the player will try to stand on it.
        assertTrue(CourseDecorator.NEAR_Z > GenContext.LANE_HALF_WIDTH + 0,
                "decoration would sit inside the lane");
        assertTrue(CourseDecorator.NEAR_Z - GenContext.LANE_HALF_WIDTH >= 1,
                "decoration needs a clear column between it and the play plane");
    }

    @Test
    void decorationStaysInsideTheClearedVolume() {
        // CourseWriter clears a fixed half-width around the lane. Anything drawn beyond that would
        // be written into terrain that was never cleared, which is how a course ends up with
        // scenery embedded in a hillside.
        assertTrue(CourseDecorator.FAR_Z <= 3,
                "decoration must stay inside CourseWriter's cleared half-width");
        assertTrue(CourseDecorator.FAR_Z >= CourseDecorator.NEAR_Z);
    }

    /** Every theme actually decorates: an undecorated theme is the bug this class exists to fix. */
    @Test
    void everyThemePutsSomethingBehindTheLane() {
        for (CourseTheme theme : CourseTheme.values()) {
            Set<Integer> depths = new HashSet<>();
            for (long seed = 0; seed < 12; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, 240, 3, seed);
                c.canvas().blocks().forEach((key, state) -> {
                    int z = CourseCanvas.unpackZ(key);
                    if (Math.abs(z) >= CourseDecorator.NEAR_Z) {
                        depths.add(z);
                    }
                });
            }
            assertTrue(!depths.isEmpty(), theme + " generated no scenery at all");
        }
    }
}
