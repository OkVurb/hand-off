package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The interior stretch has to actually appear in a course.
 *
 * <p>This project's characteristic bug is finished, correct, tested work that no player can reach:
 * an unreachable flagpole top, spike blocks nothing placed, a boss nothing spawned, four themes
 * with no set piece. Sub-environments are the same shape of risk -- the composer can be told to
 * swap contexts halfway through and still produce a course that looks identical to the old one,
 * and nothing in the existing suite would notice, because a course made entirely of surface
 * segments walks from spawn to flag perfectly well.
 *
 * <p>So this checks for the evidence rather than the intent: blocks that only the underground
 * palette places, appearing somewhere in the middle of a course whose own theme is not underground.
 */
class SubEnvironmentTest {

    private static final int LENGTH = 720;
    private static final int SEEDS = 40;

    @Test
    @DisplayName("a surface course drops into an interior somewhere in its middle")
    void coursesGoUnderground() {
        int withInterior = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c =
                    CourseComposer.compose(CourseTheme.GRASS, LENGTH, 3, seed);
            if (hasInteriorFill(c)) {
                withInterior++;
            }
        }
        assertTrue(withInterior > 0,
                "no grass course over " + SEEDS + " seeds ever went underground; the interior "
                        + "span is wired but never reached");
    }

    @Test
    @DisplayName("but not every course does, or the transition stops being an event")
    void notEveryCourse() {
        int withInterior = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c =
                    CourseComposer.compose(CourseTheme.GRASS, LENGTH, 3, seed);
            if (hasInteriorFill(c)) {
                withInterior++;
            }
        }
        assertTrue(withInterior < SEEDS,
                "every single course went underground; a transition that always happens is not a "
                        + "transition, it is the level");
    }

    /**
     * Whether the canvas contains fill only the underground palette places.
     *
     * <p>Deepstone is the marker: it is the underground fill for a world with no tint of its own,
     * and no surface palette uses it.
     */
    private static boolean hasInteriorFill(CourseComposer.Composition c) {
        var deepstone = ModBlocks.COURSE_DEEPSTONE.get();
        var canvas = c.canvas();
        for (int x = 0; x < LENGTH; x++) {
            for (int y = -6; y < 12; y++) {
                for (int z = 0; z <= 4; z++) {
                    var state = canvas.get(x, y, z);
                    if (state != null && state.is(deepstone)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
