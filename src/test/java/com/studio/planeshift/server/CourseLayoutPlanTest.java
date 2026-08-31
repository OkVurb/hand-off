package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CourseLayoutPlanTest {

    private static final int COURSE_LENGTH = 144;

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("every generated course protects its start and finish")
    void startAndFinishAlwaysHaveGround(CourseTheme theme) {
        CourseLayoutPlan plan = CourseLayoutPlan.forTheme(theme, COURSE_LENGTH);

        for (int offset = -4; offset <= 14; offset++) {
            assertTrue(plan.hasGroundAt(offset), "spawn approach at " + offset);
        }
        for (int offset = COURSE_LENGTH - 14; offset <= COURSE_LENGTH + 6; offset++) {
            assertTrue(plan.hasGroundAt(offset), "finish approach at " + offset);
        }
    }

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("every theme includes bounded jump gaps away from safe zones")
    void themesHavePlayableGaps(CourseTheme theme) {
        CourseLayoutPlan plan = CourseLayoutPlan.forTheme(theme, COURSE_LENGTH);

        for (int[] gap : plan.gaps()) {
            assertTrue(gap[0] > 14, "gap must follow spawn safety");
            assertTrue(gap[1] < COURSE_LENGTH - 14, "gap must precede finish safety");
            assertTrue(gap[1] - gap[0] + 1 <= 4, "gap must be jumpable");
            for (int offset = gap[0]; offset <= gap[1]; offset++) {
                assertFalse(plan.hasGroundAt(offset), "gap tile at " + offset);
            }
        }
    }
}
