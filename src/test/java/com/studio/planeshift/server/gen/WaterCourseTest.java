package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModFluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A water course has to actually contain water, and has to start out of it.
 *
 * <p>The theme spent an iteration as a dry course with a marine palette -- correct blocks, correct
 * set piece, no water anywhere -- which is the failure this project keeps producing and which no
 * existing test would have caught, because a dry course walks from spawn to flag perfectly well.
 *
 * <p>The second assertion is the one that took two test failures to arrive at. Flooding everything
 * drowned the spawn and filled the finish staircase, and the reference agrees with the tests: a
 * water level begins on a dry ledge and descends into the water, so entering it is a moment in the
 * level rather than the state it opens in.
 */
class WaterCourseTest {

    private static final int LENGTH = 720;
    private static final int SEEDS = 8;

    @Test
    @DisplayName("a water course is filled with water")
    void waterCoursesHoldWater() {
        for (long seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c =
                    CourseComposer.compose(CourseTheme.WATER, LENGTH, 3, seed);
            assertTrue(countWater(c, 0, LENGTH) > 0,
                    "water course on seed " + seed + " contained no water at all");
        }
    }

    @Test
    @DisplayName("but the spawn is dry, so the player enters the water rather than starting in it")
    void theSpawnIsDry() {
        for (long seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c =
                    CourseComposer.compose(CourseTheme.WATER, LENGTH, 3, seed);
            assertFalse(countWater(c, -4, 8) > 0,
                    "water reached the spawn apron on seed " + seed);
        }
    }

    /**
     * A dry theme may have a flooded stretch, but never a flooded course.
     *
     * <p>This test used to say "no other theme floods", which was true when it was written and
     * stopped being true when courses gained a sunken sub-environment -- the plan's §4.1 asks for
     * "dry ledge, then a descent into water" and §4.2 says the theme list is not a partition. The
     * invariant worth keeping is the one that was actually being protected: a grass course must not
     * <em>be</em> a water course. A stretch in the middle is a change of scene; the whole level
     * underwater is a different level.
     *
     * <p>The spawn apron is checked separately below and stays dry for every theme, so the player
     * never starts in water they did not choose to enter.
     */
    @Test
    @DisplayName("a dry theme may have a wet stretch but is never wet end to end")
    void otherThemesFloodOnlyInPart() {
        for (CourseTheme theme : CourseTheme.values()) {
            if (theme == CourseTheme.WATER) {
                continue;
            }
            for (long seed = 0; seed < SEEDS; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, LENGTH, 3, seed);
                int wet = countWater(c, 0, LENGTH);
                if (wet == 0) {
                    continue;
                }
                assertFalse(countWater(c, -4, 8) > 0,
                        theme + " flooded its own spawn apron on seed " + seed);
                // The sunken stretch runs from about a third to about two thirds of the course.
                // Anything approaching the whole length means the theme has stopped being itself.
                assertFalse(countWater(c, 0, LENGTH / 8) > 0 && countWater(c, LENGTH * 7 / 8, LENGTH) > 0,
                        theme + " course on seed " + seed + " is wet at both ends; that is a water "
                                + "course wearing another theme's palette");
            }
        }
    }

    private static int countWater(CourseComposer.Composition c, int fromX, int toX) {
        var water = ModFluids.WATER_BLOCK.get();
        int found = 0;
        for (int x = fromX; x < toX; x++) {
            for (int y = -8; y < 24; y++) {
                for (int z = -1; z <= 1; z++) {
                    var state = c.canvas().get(x, y, z);
                    if (state != null && state.is(water)) {
                        found++;
                    }
                }
            }
        }
        return found;
    }
}
