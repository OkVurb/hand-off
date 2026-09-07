package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModEntities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Volcanic bombs belong to the volcano and nowhere else.
 *
 * <p>The justification for a rock falling out of the sky is the erupting background the lava theme
 * already draws. In a grass level it is an unexplained thing landing on the player, which is the
 * definition of an unfair hazard.
 *
 * <p>Worth a test rather than a comment because the gate that enforces it very nearly did nothing:
 * theme fit was only consulted for set pieces, so an ordinary theme-specific segment would have
 * been placed everywhere and compiled, tested and shipped perfectly happily.
 */
class EruptionFieldTest {

    private static final int LENGTH = 720;
    private static final int SEEDS = 25;

    private static int coursesWithBombs(CourseTheme theme) {
        int found = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c = CourseComposer.compose(theme, LENGTH, 3, seed);
            if (c.canvas().entities().stream()
                    .anyMatch(e -> e.type() == ModEntities.VOLCANIC_BOMB.get())) {
                found++;
            }
        }
        return found;
    }

    @Test
    @DisplayName("bombs fall in the volcano")
    void bombsReachLavaCourses() {
        assertTrue(coursesWithBombs(CourseTheme.LAVA) > 0,
                "no lava course contained a volcanic bomb; the segment is catalogued and nothing "
                        + "places it");
    }

    @Test
    @DisplayName("and nowhere else")
    void bombsStayOutOfOtherThemes() {
        for (CourseTheme theme : CourseTheme.values()) {
            if (theme == CourseTheme.LAVA) {
                continue;
            }
            assertEquals(0, coursesWithBombs(theme),
                    theme + " courses contained volcanic bombs; a rock out of a clear sky is an "
                            + "unexplained hazard");
        }
    }
}
