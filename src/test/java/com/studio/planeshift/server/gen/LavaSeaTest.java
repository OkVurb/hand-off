package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModFluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A volcano course is played above one lava sea, not over a series of puddles.
 *
 * <p>Two things are worth pinning. The sea has to be <em>continuous</em>, because the whole point
 * of the finding is that every gap in the floor is a window onto the same danger rather than a new
 * one. And it has to sit at a <em>single</em> depth: a per-column depth follows the terrain up and
 * down, which is a lava river with hills in it, and a liquid finds one level.
 */
class LavaSeaTest {

    private static final int LENGTH = 240;

    private static CourseCanvas lavaCourse(long seed) {
        return CourseComposer.compose(CourseTheme.LAVA, LENGTH, 3, seed).canvas();
    }

    /**
     * The deepest lava in a course runs almost the whole width of it.
     *
     * <p>Written this way rather than as "all the lava is at one height", which is what the first
     * version of this test asserted and which turned out to be false for a good reason: set-piece
     * segments build raised lava channels of their own -- a bridge with a moat under it -- and the
     * reference has those too. The finding was never "there is only ever one lava height"; it was
     * that the <em>bottom</em> of a volcano level is one continuous surface rather than a row of
     * unrelated holes. So that is what is checked.
     */
    @Test
    @DisplayName("the floor of the level is one continuous sea")
    void theSeaRunsTheWholeCourse() {
        for (long seed = 0; seed < 8; seed++) {
            CourseCanvas canvas = lavaCourse(seed);
            int seaY = deepestLava(canvas);
            assertTrue(seaY != Integer.MIN_VALUE, "seed " + seed + " has no lava at all");
            int gaps = 0;
            for (int x = 0; x < LENGTH; x++) {
                if (!ModFluids.LAVA_BLOCK.get().equals(blockAt(canvas, x, seaY))) {
                    gaps++;
                }
            }
            // Not zero-tolerance: the sea fills only what is still empty, so a segment that built
            // its own floor down at this depth legitimately interrupts it. What must not come back
            // is the old behaviour, where lava existed under gaps and nowhere else.
            assertTrue(gaps < LENGTH / 4, "seed " + seed + ": " + gaps + " of " + LENGTH
                    + " columns have no sea; that is puddles, not a sea");
        }
    }

    /** The lowest y at which any lava sits, or MIN_VALUE if the course has none. */
    private static int deepestLava(CourseCanvas canvas) {
        for (int y = -40; y < 40; y++) {
            for (int x = 0; x < LENGTH; x++) {
                if (ModFluids.LAVA_BLOCK.get().equals(blockAt(canvas, x, y))) {
                    return y;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    @Test
    @DisplayName("and only in the volcano")
    void otherThemesGetNoSea() {
        for (CourseTheme theme : CourseTheme.values()) {
            if (theme == CourseTheme.LAVA) {
                continue;
            }
            CourseCanvas canvas = CourseComposer.compose(theme, LENGTH, 3, 1L).canvas();
            int found = 0;
            for (int x = 0; x < LENGTH; x++) {
                for (int y = -40; y < 40; y++) {
                    if (ModFluids.LAVA_BLOCK.get().equals(blockAt(canvas, x, y))) {
                        found++;
                    }
                }
            }
            assertEquals(0, found, theme + " courses should not be flooded with lava");
        }
    }

    /**
     * The sea emits, and only where it can be seen doing it.
     *
     * <p>A hazard that never fires is scenery the player learns to ignore, which is worse than not
     * having one. A Podoboo under a solid floor is exactly that: it rises, hits the underside of
     * the level and falls back, having cost nothing and taught nothing.
     */
    @Test
    @DisplayName("the sea throws fireballs and geysers")
    void theSeaEmits() {
        int coursesWithPodoboos = 0;
        for (long seed = 0; seed < 8; seed++) {
            CourseCanvas canvas = lavaCourse(seed);
            int seaY = deepestLava(canvas);
            long fromTheSea = canvas.entities().stream()
                    .filter(e -> e.type() == com.studio.planeshift.common.registry.ModEntities
                            .PODOBOO.get()
                            || e.type() == com.studio.planeshift.common.registry.ModEntities
                            .LAVA_JET.get())
                    .filter(e -> Math.abs(e.y() - (seaY + 1.0D)) < 0.001D)
                    .count();
            if (fromTheSea > 0) {
                coursesWithPodoboos++;
            }
        }
        assertTrue(coursesWithPodoboos >= 6,
                "only " + coursesWithPodoboos + " of 8 volcano courses had fireballs rising out of "
                        + "the sea; the surface is decoration in the rest");
    }

    /**
     * The sideways jets exist in the world, not only in the class.
     *
     * <p>Written the moment the orientation was built, because the alternative is this project's
     * signature failure: a hazard that compiles, renders, is tested at the unit level and is
     * generated by nothing. The sea's own jets all point up, so a jet with a horizontal facing can
     * only have come from the nozzle corridor.
     */
    @Test
    @DisplayName("wall nozzles fire sideways somewhere in the volcano")
    void nozzlesAreGenerated() {
        boolean found = false;
        for (long seed = 0; seed < 40 && !found; seed++) {
            CourseCanvas canvas = CourseComposer.compose(CourseTheme.LAVA, 720, 3, seed).canvas();
            found = canvas.entities().stream()
                    .anyMatch(e -> e.type() == com.studio.planeshift.common.registry.ModEntities
                            .LAVA_JET.get() && e.configure() != null);
        }
        assertTrue(found, "no volcano course in forty seeds contained a wall-mounted jet; the "
                + "sideways orientation is built and nothing generates one");
    }

    private static net.minecraft.world.level.block.Block blockAt(CourseCanvas canvas, int x, int y) {
        var state = canvas.get(x, y, 0);
        return state == null ? null : state.getBlock();
    }
}
