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

    @Test
    @DisplayName("no other theme floods")
    void onlyWaterFloods() {
        for (CourseTheme theme : CourseTheme.values()) {
            if (theme == CourseTheme.WATER) {
                continue;
            }
            CourseComposer.Composition c = CourseComposer.compose(theme, LENGTH, 3, 1L);
            assertFalse(countWater(c, 0, LENGTH) > 0,
                    theme + " course was flooded and should not have been");
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
