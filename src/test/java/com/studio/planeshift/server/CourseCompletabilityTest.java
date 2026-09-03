package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.studio.planeshift.common.course.CourseLayout;
import com.studio.planeshift.common.course.CourseTheme;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backlog item 71: prove every generated course is clearable, rather than assuming it.
 *
 * <p>The existing layout tests only exercise {@code forTheme}, which pins the world number to 1.
 * Difficulty scaling widens pits with the world number, so the hardest courses in the game — the
 * ones a player reaches after hours — travel a code path nothing had ever checked. This sweeps
 * every theme, every world, a spread of lengths and many seeds, and asserts the properties that
 * make a course finishable at all.
 *
 * <p>The properties are deliberately about the *layout*, not about physics: a test that models
 * Minecraft's jump arc would be a second implementation of the movement code and would drift from
 * it. {@link CourseLayout#JUMPABLE_LIMIT} is the contract the generator declares, so the test
 * holds the generator to its own promise.
 */
class CourseCompletabilityTest {

    private static final int[] LENGTHS = {64, 96, 144, 180, 224};
    private static final int SEEDS = 40;

    /** One failure, described well enough to reproduce without rerunning the sweep. */
    private record Failure(CourseTheme theme, int world, int length, int seed, String what) {
        @Override
        public String toString() {
            return String.format("%s world %d length %d seed %d: %s", theme, world, length, seed, what);
        }
    }

    private static CourseLayoutPlan plan(CourseTheme theme, int length, int seed, int world) {
        return CourseLayoutPlan.build(theme, length, CourseLayout.DEFAULT, seed, world);
    }

    /**
     * The property that matters most: no run of missing ground wider than the generator's own
     * declared jumpable limit. Checking pits one at a time misses this — two legal pits with too
     * little ground between them merge into one illegal one.
     */
    @Test
    @DisplayName("no generated course anywhere contains an unjumpable hole")
    void noUnjumpableHoleInAnyCourse() {
        List<Failure> failures = new ArrayList<>();

        for (CourseTheme theme : CourseTheme.values()) {
            for (int world = 1; world <= 5; world++) {
                for (int length : LENGTHS) {
                    for (int seed = 0; seed < SEEDS; seed++) {
                        CourseLayoutPlan p = plan(theme, length, seed, world);
                        int run = 0;
                        int worst = 0;
                        for (int offset = 0; offset <= length; offset++) {
                            run = p.hasGroundAt(offset) ? 0 : run + 1;
                            worst = Math.max(worst, run);
                        }
                        if (worst > CourseLayout.JUMPABLE_LIMIT) {
                            failures.add(new Failure(theme, world, length, seed,
                                    worst + " block hole, limit is " + CourseLayout.JUMPABLE_LIMIT));
                        }
                    }
                }
            }
        }

        report(failures);
    }

    @Test
    @DisplayName("no generated course strands its checkpoint over a pit")
    void checkpointAlwaysStandsOnGround() {
        List<Failure> failures = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (int world = 1; world <= 5; world++) {
                for (int length : LENGTHS) {
                    for (int seed = 0; seed < SEEDS; seed++) {
                        CourseLayoutPlan p = plan(theme, length, seed, world);
                        if (!p.hasGroundAt(p.midpoint())) {
                            failures.add(new Failure(theme, world, length, seed,
                                    "checkpoint at " + p.midpoint() + " floats over a pit"));
                        }
                    }
                }
            }
        }
        report(failures);
    }

    @Test
    @DisplayName("no generated course opens a pit under the spawn or the flagpole")
    void spawnAndFinishAlwaysSolid() {
        List<Failure> failures = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (int world = 1; world <= 5; world++) {
                for (int length : LENGTHS) {
                    for (int seed = 0; seed < SEEDS; seed++) {
                        CourseLayoutPlan p = plan(theme, length, seed, world);
                        for (int offset = -4; offset <= CourseLayout.DEFAULT_SAFE_MARGIN; offset++) {
                            if (!p.hasGroundAt(offset)) {
                                failures.add(new Failure(theme, world, length, seed,
                                        "pit at spawn offset " + offset));
                                break;
                            }
                        }
                        for (int offset = length - CourseLayout.DEFAULT_SAFE_MARGIN;
                                offset <= length + 6; offset++) {
                            if (!p.hasGroundAt(offset)) {
                                failures.add(new Failure(theme, world, length, seed,
                                        "pit at finish offset " + offset));
                                break;
                            }
                        }
                    }
                }
            }
        }
        report(failures);
    }

    /**
     * Consecutive pits must leave somewhere to land. A one-block ledge between two pits is
     * technically ground and practically a second pit.
     */
    @Test
    @DisplayName("consecutive pits always leave a landable run of ground")
    void pitsLeaveSomewhereToLand() {
        List<Failure> failures = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (int world = 1; world <= 5; world++) {
                for (int length : LENGTHS) {
                    for (int seed = 0; seed < SEEDS; seed++) {
                        CourseLayoutPlan p = plan(theme, length, seed, world);
                        int[][] gaps = p.gaps();
                        for (int i = 1; i < gaps.length; i++) {
                            int ground = gaps[i][0] - gaps[i - 1][1] - 1;
                            if (ground < CourseLayoutPlan.MIN_GROUND_RUN) {
                                failures.add(new Failure(theme, world, length, seed,
                                        "only " + ground + " blocks of ground between pits "
                                                + (i - 1) + " and " + i));
                                break;
                            }
                        }
                    }
                }
            }
        }
        report(failures);
    }

    /** Difficulty should still produce a course, not an empty corridor or a chasm. */
    @Test
    @DisplayName("harder worlds add pressure without removing the course")
    void harderWorldsStayCourses() {
        for (int world = 1; world <= 5; world++) {
            CourseLayoutPlan p = plan(CourseTheme.GRASS, 144, 7, world);
            assertTrue(p.gaps().length > 0, "world " + world + " generated a corridor with no pits");
            assertTrue(p.setPieceCount() >= 3, "world " + world + " generated almost no content");
        }
    }

    private static void report(List<Failure> failures) {
        if (failures.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder(failures.size() + " unclearable course(s):\n");
        failures.stream().limit(15).forEach(f -> message.append("  ").append(f).append('\n'));
        if (failures.size() > 15) {
            message.append("  ...and ").append(failures.size() - 15).append(" more\n");
        }
        fail(message.toString());
    }
}
