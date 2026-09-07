package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModEntities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The saw has to actually turn up in courses.
 *
 * <p>This project's recurring bug is content that is finished, correct and unreachable: a boss
 * nothing spawned, spike blocks nothing placed, four themes with no set piece. A hazard entity is
 * particularly easy to add that way, because everything about it compiles and tests green whether
 * or not a single course ever contains one.
 *
 * <p>Both bounds again. It appears, and it does not appear in every course -- a hazard the player
 * meets in every level stops being a hazard and becomes the floor.
 */
class SawPlacementTest {

    private static final int LENGTH = 720;
    private static final int SEEDS = 30;

    private static int coursesWithSaw() {
        int found = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c =
                    CourseComposer.compose(CourseTheme.LAVA, LENGTH, 3, seed);
            boolean has = c.canvas().entities().stream()
                    .anyMatch(e -> e.type() == ModEntities.SAW.get());
            if (has) {
                found++;
            }
        }
        return found;
    }

    @Test
    @DisplayName("saws reach the player")
    void sawsAppear() {
        assertTrue(coursesWithSaw() > 0,
                "no course over " + SEEDS + " seeds contained a saw; it is registered, rendered "
                        + "and catalogued, and nothing places it");
    }

    @Test
    @DisplayName("but not in every course")
    void notEverywhere() {
        assertTrue(coursesWithSaw() < SEEDS,
                "every course had a saw; a hazard met every single level is the floor, not a "
                        + "hazard");
    }
}
