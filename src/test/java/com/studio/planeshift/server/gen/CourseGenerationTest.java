package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The proof the whole generator architecture exists to make possible.
 *
 * <p>Courses used to be written straight into the world as they were decided, which meant the only
 * way to know whether one was playable was to load it and walk it. Nobody did, so courses shipped
 * with pits nobody could cross. Composition now produces a {@link CourseCanvas} — plain data — so a
 * test can flood-fill the finished level with a conservative model of the player's jump and prove
 * the flag is reachable from the spawn.
 *
 * <p>This is not a sampling check. It sweeps every theme, every difficulty, several lengths and
 * many seeds, because the failing course is by definition the one nobody thought to try.
 */
class CourseGenerationTest {

    private static final int[] LENGTHS = {96, 144, 180, 224};
    private static final int SEEDS = 25;

    private static CourseComposer.Composition compose(CourseTheme theme, int length, int difficulty,
                                                      long seed) {
        return CourseComposer.compose(theme, length, difficulty, seed);
    }

    /** One failing course, described well enough to reproduce it without rerunning the sweep. */
    private record Failure(CourseTheme theme, int difficulty, int length, long seed, String what) {
        @Override
        public String toString() {
            return String.format("%s d%d len%d seed%d: %s", theme, difficulty, length, seed, what);
        }
    }

    private static void report(List<Failure> failures, int total) {
        if (failures.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder(
                failures.size() + " of " + total + " generated courses failed:\n");
        failures.stream().limit(12).forEach(f -> message.append("  ").append(f).append('\n'));
        if (failures.size() > 12) {
            message.append("  ...and ").append(failures.size() - 12).append(" more\n");
        }
        fail(message.toString());
    }

    // ------------------------------------------------------------------ the big one

    @Test
    @DisplayName("every generated course can actually be walked from spawn to flag")
    void everyCourseIsWalkable() {
        List<Failure> failures = new ArrayList<>();
        int total = 0;

        for (CourseTheme theme : CourseTheme.values()) {
            for (int difficulty = 0; difficulty <= 4; difficulty++) {
                for (int length : LENGTHS) {
                    for (int seed = 0; seed < SEEDS; seed++) {
                        total++;
                        CourseComposer.Composition c = compose(theme, length, difficulty, seed);
                        CourseReachability reach = new CourseReachability(c.canvas(), 0);
                        CourseReachability.Result result = reach.search(0, c.spawnY(), c.flagX());
                        if (!result.reachable()) {
                            failures.add(new Failure(theme, difficulty, length, seed,
                                    result.describe(c.flagX())
                                            + " segments=" + c.segmentIds()));
                        }
                    }
                }
            }
        }
        report(failures, total);
    }

    // ------------------------------------------------------------------ structure

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("the spawn is on solid ground with room to stand")
    void spawnIsStandable(CourseTheme theme) {
        for (int seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c = compose(theme, 144, 2, seed);
            CourseReachability reach = new CourseReachability(c.canvas(), 0);
            assertTrue(reach.isStand(0, c.spawnY()),
                    theme + " seed " + seed + ": nothing to stand on at the spawn");
        }
    }

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("the flag is planted on solid ground, not floating")
    void flagIsGrounded(CourseTheme theme) {
        for (int seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c = compose(theme, 144, 2, seed);
            CourseCanvas canvas = c.canvas();
            boolean anyPole = false;
            for (int y = -12; y <= 24; y++) {
                if (canvas.get(c.flagX(), y, 0) != null) {
                    anyPole = true;
                    break;
                }
            }
            assertTrue(anyPole, theme + " seed " + seed + ": no flagpole was placed");
        }
    }

    @ParameterizedTest
    @EnumSource(CourseTheme.class)
    @DisplayName("every course places exactly three star coins")
    void starCoinsArePlaced(CourseTheme theme) {
        for (int seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c = compose(theme, 144, 2, seed);
            long stars = c.canvas().items().stream()
                    .filter(i -> i.item().toString().contains("star_coin"))
                    .count();
            assertEquals(3, stars,
                    theme + " seed " + seed + ": star coin count wrong — the progress system "
                            + "tracks three per course and cannot show what was never placed");
        }
    }

    // ------------------------------------------------------------------ design rules

    /**
     * The teaching rule. A demanding segment must never introduce a mechanic: the player has to
     * have met donut blocks somewhere safe before meeting them over a pit.
     */
    @Test
    @DisplayName("no hard segment introduces a mechanic the course has not taught")
    void hardSegmentsNeverIntroduceMechanics() {
        List<Failure> failures = new ArrayList<>();
        int total = 0;

        for (CourseTheme theme : CourseTheme.values()) {
            for (int seed = 0; seed < SEEDS; seed++) {
                total++;
                CourseComposer.Composition c = compose(theme, 180, 4, seed);
                Set<Segment.Tag> taught = new HashSet<>();
                for (String id : c.segmentIds()) {
                    Segment segment = byId(id);
                    if (segment == null) {
                        continue;
                    }
                    Segment.SegmentSpec s = segment.spec();
                    if (s.difficulty() >= 3) {
                        for (Segment.Tag tag : s.tags()) {
                            if (tag == Segment.Tag.REST || tag == Segment.Tag.SECRET
                                    || tag == Segment.Tag.SETPIECE) {
                                continue;
                            }
                            if (!taught.contains(tag)) {
                                failures.add(new Failure(theme, 4, 180, seed,
                                        id + " uses " + tag + " before anything taught it"));
                            }
                        }
                    }
                    taught.addAll(s.tags());
                }
            }
        }
        report(failures, total);
    }

    /** Nothing demanding should run straight into something else demanding. */
    @Test
    @DisplayName("a hard segment is always followed by a breather")
    void hardSegmentsAreFollowedByRest() {
        List<Failure> failures = new ArrayList<>();
        int total = 0;

        for (CourseTheme theme : CourseTheme.values()) {
            for (int seed = 0; seed < SEEDS; seed++) {
                total++;
                CourseComposer.Composition c = compose(theme, 180, 4, seed);
                List<String> ids = c.segmentIds();
                for (int i = 0; i < ids.size() - 1; i++) {
                    Segment current = byId(ids.get(i));
                    Segment next = byId(ids.get(i + 1));
                    if (current == null || next == null) {
                        continue;
                    }
                    if (current.spec().difficulty() >= 3
                            && !next.spec().has(Segment.Tag.REST)) {
                        failures.add(new Failure(theme, 4, 180, seed,
                                ids.get(i) + " (hard) is followed by " + ids.get(i + 1)));
                    }
                }
            }
        }
        report(failures, total);
    }

    @Test
    @DisplayName("a course is not the same segment over and over")
    void coursesUseVariedSegments() {
        for (CourseTheme theme : CourseTheme.values()) {
            for (int seed = 0; seed < 10; seed++) {
                CourseComposer.Composition c = compose(theme, 180, 2, seed);
                Set<String> distinct = new HashSet<>(c.segmentIds());
                assertTrue(distinct.size() >= 5,
                        theme + " seed " + seed + " used only " + distinct.size()
                                + " distinct segments: " + c.segmentIds());
            }
        }
    }

    /** Two seeds must not produce the same level, or the seeding is decorative. */
    @Test
    @DisplayName("different seeds produce different courses")
    void seedsProduceDifferentCourses() {
        for (CourseTheme theme : CourseTheme.values()) {
            List<String> first = compose(theme, 144, 2, 1L).segmentIds();
            List<String> second = compose(theme, 144, 2, 2L).segmentIds();
            assertFalse(first.equals(second),
                    theme + ": seeds 1 and 2 generated an identical course");
        }
    }

    /** The same seed must produce the same level, or retrying after a death is a new level. */
    @Test
    @DisplayName("the same seed always produces the identical course")
    void generationIsDeterministic() {
        for (CourseTheme theme : CourseTheme.values()) {
            CourseComposer.Composition a = compose(theme, 144, 3, 99L);
            CourseComposer.Composition b = compose(theme, 144, 3, 99L);
            assertEquals(a.segmentIds(), b.segmentIds(), theme + ": segment order differed");
            assertEquals(a.canvas().blockCount(), b.canvas().blockCount(),
                    theme + ": block count differed");
            assertEquals(a.flagX(), b.flagX(), theme + ": flag moved");
        }
    }

    @Test
    @DisplayName("the castle set piece appears only where the theme earns it")
    void setPieceIsThemeAppropriate() {
        for (CourseTheme theme : CourseTheme.values()) {
            for (int seed = 0; seed < SEEDS; seed++) {
                CourseComposer.Composition c = compose(theme, 224, 4, seed);
                boolean castle = c.segmentIds().contains("castle_bridge");
                if (castle && theme != CourseTheme.LAVA && theme != CourseTheme.UNDERGROUND) {
                    fail(theme + " seed " + seed + " built a castle bridge in the wrong biome");
                }
            }
        }
    }

    @Test
    @DisplayName("at most one set piece per course")
    void oneSetPiecePerCourse() {
        for (int seed = 0; seed < SEEDS; seed++) {
            CourseComposer.Composition c = compose(CourseTheme.LAVA, 224, 4, seed);
            long count = c.segmentIds().stream().filter(id -> id.equals("castle_bridge")).count();
            assertTrue(count <= 1, "seed " + seed + " placed " + count + " set pieces");
        }
    }

    private static Segment byId(String id) {
        for (Segment segment : SegmentLibrary.all()) {
            if (segment.spec().id().equals(id)) {
                return segment;
            }
        }
        for (Segment segment : SegmentLibrary.setPieces()) {
            if (segment.spec().id().equals(id)) {
                return segment;
            }
        }
        return null;
    }
}
