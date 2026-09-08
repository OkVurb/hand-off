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

    /**
     * Some courses descend into water rather than into a cave.
     *
     * <p>The entry's own wording is "outdoor to cave to outdoor; <em>or</em> dry ledge, then a
     * descent into water", and only the first was built -- which made the middle of every course
     * the same kind of change. It is also the cheapest crossbreed in §4.2: water stops being a
     * place the player travels to and becomes something that happened to part of a level they were
     * already in.
     */
    @Test
    @DisplayName("and some of them descend into water instead")
    void someCoursesFlood() {
        int sunken = 0;
        for (long seed = 0; seed < 40; seed++) {
            CourseCanvas canvas = CourseComposer.compose(
                    CourseTheme.GRASS, 720, 3, seed).canvas();
            boolean wet = false;
            for (int x = 0; x < 720 && !wet; x++) {
                for (int y = -8; y < 20; y++) {
                    var state = canvas.get(x, y, 0);
                    if (state != null && state.getBlock()
                            == com.studio.planeshift.common.registry.ModFluids.WATER_BLOCK.get()) {
                        wet = true;
                        break;
                    }
                }
            }
            if (wet) {
                sunken++;
            }
        }
        assertTrue(sunken > 0, "no grass course in forty seeds had a flooded stretch");
        assertTrue(sunken < 40, "every grass course flooded; a transition that happens every time "
                + "stops being an event, which is the rule the cave stretch already follows");
    }
}
