package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseLayout;
import com.studio.planeshift.common.course.CourseTheme;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CourseLayoutPlanTest {

    private static final int COURSE_LENGTH = 144;

    private static CourseLayout layout(Optional<List<CourseLayout.Gap>> gaps,
                                       Optional<Integer> gapCount, int maxGapWidth) {
        return new CourseLayout(gaps, gapCount, maxGapWidth,
                CourseLayout.DEFAULT_SAFE_MARGIN, CourseLayout.DEFAULT_SAFE_MARGIN,
                Optional.empty(), Optional.empty());
    }

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("every generated course protects its start and finish")
    void startAndFinishAlwaysHaveGround(CourseTheme theme) {
        CourseLayoutPlan plan = CourseLayoutPlan.forTheme(theme, COURSE_LENGTH);

        for (int offset = -4; offset <= CourseLayout.DEFAULT_SAFE_MARGIN; offset++) {
            assertTrue(plan.hasGroundAt(offset), "spawn approach at " + offset);
        }
        for (int offset = COURSE_LENGTH - CourseLayout.DEFAULT_SAFE_MARGIN;
                offset <= COURSE_LENGTH + 6; offset++) {
            assertTrue(plan.hasGroundAt(offset), "finish approach at " + offset);
        }
    }

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("every theme includes bounded jump gaps away from safe zones")
    void themesHavePlayableGaps(CourseTheme theme) {
        CourseLayoutPlan plan = CourseLayoutPlan.forTheme(theme, COURSE_LENGTH);

        assertTrue(plan.gaps().length > 0, "a course with no pits is a corridor");
        for (int[] gap : plan.gaps()) {
            assertTrue(gap[0] > CourseLayout.DEFAULT_SAFE_MARGIN, "gap must follow spawn safety");
            assertTrue(gap[1] < COURSE_LENGTH - CourseLayout.DEFAULT_SAFE_MARGIN,
                    "gap must precede finish safety");
            assertTrue(gap[1] - gap[0] + 1 <= CourseLayout.JUMPABLE_LIMIT, "gap must be jumpable");
            for (int offset = gap[0]; offset <= gap[1]; offset++) {
                assertFalse(plan.hasGroundAt(offset), "gap tile at " + offset);
            }
        }
    }

    /**
     * The regression this class was rewritten for. The old per-theme offset table pushed two of
     * its six hard-coded pits into contact at the higher theme offsets, producing a fourteen-block
     * continuous hole in ghost-house courses. Individually each pit passed the width check, so
     * checking pits one at a time never caught it; only the merged run does.
     */
    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("no two pits merge into an unjumpable run")
    void adjacentGapsNeverMerge(CourseTheme theme) {
        for (int length : new int[]{64, 96, 144, 180, 224}) {
            CourseLayoutPlan plan = CourseLayoutPlan.forTheme(theme, length);
            int run = 0;
            for (int offset = 0; offset <= length; offset++) {
                run = plan.hasGroundAt(offset) ? 0 : run + 1;
                assertTrue(run <= CourseLayout.JUMPABLE_LIMIT,
                        theme + " at length " + length + " has a " + run + " block hole ending at "
                                + offset);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("consecutive pits keep a landable run of ground between them")
    void gapsKeepGroundBetweenThem(CourseTheme theme) {
        CourseLayoutPlan plan = CourseLayoutPlan.forTheme(theme, COURSE_LENGTH);
        int[][] gaps = plan.gaps();
        for (int i = 1; i < gaps.length; i++) {
            assertTrue(gaps[i][0] - gaps[i - 1][1] > CourseLayoutPlan.MIN_GROUND_RUN,
                    "pits " + (i - 1) + " and " + i + " leave no landing");
        }
    }

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("the checkpoint always stands on solid ground")
    void checkpointHasGround(CourseTheme theme) {
        for (int length : new int[]{64, 96, 144, 180, 224}) {
            CourseLayoutPlan plan = CourseLayoutPlan.forTheme(theme, length);
            assertTrue(plan.hasGroundAt(plan.midpoint()),
                    theme + " at length " + length + " floats its checkpoint over a pit");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {64, 96, 144, 180, 224})
    @DisplayName("a longer course gets more pits and more set pieces, not the same ones spread out")
    void lengthChangesContentDensity(int length) {
        CourseLayoutPlan shorter = CourseLayoutPlan.forTheme(CourseTheme.GRASS, 64);
        CourseLayoutPlan plan = CourseLayoutPlan.forTheme(CourseTheme.GRASS, length);

        assertTrue(plan.gaps().length >= shorter.gaps().length,
                "pit count must not fall as the course grows");
        assertTrue(plan.setPieceCount() >= shorter.setPieceCount(),
                "set-piece count must not fall as the course grows");
        if (length >= 180) {
            assertTrue(plan.setPieceCount() > shorter.setPieceCount(),
                    "a course nearly three times as long should carry more set pieces");
        }
    }

    @Test
    @DisplayName("a course can list its own pits")
    void explicitGapsAreHonoured() {
        CourseLayoutPlan plan = CourseLayoutPlan.build(CourseTheme.GRASS, COURSE_LENGTH,
                layout(Optional.of(List.of(new CourseLayout.Gap(30, 34), new CourseLayout.Gap(50, 55))),
                        Optional.empty(), CourseLayout.JUMPABLE_LIMIT));

        assertEquals(2, plan.gaps().length);
        assertFalse(plan.hasGroundAt(32));
        assertFalse(plan.hasGroundAt(52));
        assertTrue(plan.hasGroundAt(40));
    }

    @Test
    @DisplayName("an authored pit that is unjumpable, unsafe or on the checkpoint is dropped")
    void explicitGapsAreStillValidated() {
        CourseLayoutPlan plan = CourseLayoutPlan.build(CourseTheme.GRASS, COURSE_LENGTH,
                layout(Optional.of(List.of(
                        new CourseLayout.Gap(30, 45),     // too wide to clear
                        new CourseLayout.Gap(2, 6),       // inside the spawn safe zone
                        new CourseLayout.Gap(138, 142),   // inside the finish safe zone
                        new CourseLayout.Gap(70, 74),     // straddles the midpoint checkpoint
                        new CourseLayout.Gap(50, 54))),   // the only legal one
                        Optional.empty(), CourseLayout.JUMPABLE_LIMIT));

        assertEquals(1, plan.gaps().length);
        assertEquals(50, plan.gaps()[0][0]);
        assertEquals(54, plan.gaps()[0][1]);
    }

    @Test
    @DisplayName("a course can ask for a specific number of pits")
    void gapCountIsHonoured() {
        CourseLayoutPlan plan = CourseLayoutPlan.build(CourseTheme.GRASS, 200,
                layout(Optional.empty(), Optional.of(3), CourseLayout.JUMPABLE_LIMIT));
        assertEquals(3, plan.gaps().length);
    }

    @Test
    @DisplayName("max_gap_width caps every derived pit")
    void maxGapWidthIsHonoured() {
        CourseLayoutPlan plan = CourseLayoutPlan.build(CourseTheme.LAVA, COURSE_LENGTH,
                layout(Optional.empty(), Optional.empty(), 4));
        for (int[] gap : plan.gaps()) {
            assertTrue(gap[1] - gap[0] + 1 <= 4, "pit wider than the course allows");
        }
    }

    @Test
    @DisplayName("features come from the course when given and from the theme when not")
    void featuresResolveFromCourseThenTheme() {
        CourseLayoutPlan themed = CourseLayoutPlan.forTheme(CourseTheme.LAVA, COURSE_LENGTH);
        assertTrue(themed.has(CourseLayout.Feature.CASTLE_FINALE),
                "lava keeps its castle finale by default");
        assertFalse(CourseLayoutPlan.forTheme(CourseTheme.GRASS, COURSE_LENGTH)
                .has(CourseLayout.Feature.CASTLE_FINALE));

        CourseLayoutPlan authored = CourseLayoutPlan.build(CourseTheme.GRASS, COURSE_LENGTH,
                new CourseLayout(Optional.empty(), Optional.empty(), CourseLayout.JUMPABLE_LIMIT,
                        CourseLayout.DEFAULT_SAFE_MARGIN, CourseLayout.DEFAULT_SAFE_MARGIN,
                        Optional.of(Set.of(CourseLayout.Feature.CASTLE_FINALE)), Optional.empty()));
        assertTrue(authored.has(CourseLayout.Feature.CASTLE_FINALE),
                "a grass course may still ask for a castle");
        assertFalse(authored.has(CourseLayout.Feature.CONVEYOR_GAUNTLET),
                "an explicit feature list replaces the theme default rather than adding to it");
    }
}
