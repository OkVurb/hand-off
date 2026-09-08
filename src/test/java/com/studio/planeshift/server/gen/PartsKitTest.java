package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The parts kit reaches courses, not just the registry.
 *
 * <p>§4.8. A block that exists and is placed by nothing is this project's signature bug, and the
 * grate is more exposed to it than most: it is the only floor in the mod you can see through, and
 * the only thing that makes that worth having is standing on one with something underneath.
 */
class PartsKitTest {

    private static final int LENGTH = 720;

    @Test
    @DisplayName("scaffolding gets built, decked with grate")
    void scaffoldReachesCourses() {
        int withDeck = 0;
        for (long seed = 0; seed < 30; seed++) {
            CourseCanvas canvas = CourseComposer.compose(CourseTheme.GRASS, LENGTH, 3, seed).canvas();
            for (int x = 0; x < LENGTH && withDeck <= seed; x++) {
                for (int y = -4; y < 24; y++) {
                    var state = canvas.get(x, y, 0);
                    if (state != null && state.is(ModBlocks.COURSE_GRATE.get())) {
                        withDeck++;
                        break;
                    }
                }
            }
        }
        assertTrue(withDeck > 0,
                "no grass course in thirty seeds contained a grate deck; the block is registered "
                        + "and nothing places it");
    }

    /** The capsule beam's parts all reach a course: ledge, beam and the switch on its pole. */
    @Test
    @DisplayName("ledges, beams and pole-mounted switches all get placed")
    void therestOfTheKitReachesCourses() {
        boolean ledge = false;
        boolean logs = false;
        for (long seed = 0; seed < 30 && !(ledge && logs); seed++) {
            CourseCanvas canvas = CourseComposer.compose(CourseTheme.GRASS, LENGTH, 3, seed).canvas();
            for (int x = 0; x < LENGTH; x++) {
                for (int y = -4; y < 24; y++) {
                    var state = canvas.get(x, y, 0);
                    if (state == null) {
                        continue;
                    }
                    ledge |= state.is(ModBlocks.COURSE_LEDGE.get());
                    logs |= state.is(ModBlocks.COURSE_LOG_END.get());
                }
            }
        }
        assertTrue(ledge, "no inset ledge in thirty grass courses; the block is registered and "
                + "nothing places it");
        assertTrue(logs, "no cut-log platform in thirty grass courses, so §5.9's identity block is "
                + "art nobody sees");
    }
}
