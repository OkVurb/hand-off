package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every course is about something, and its theme means something.
 *
 * <p>These are design rules rather than correctness rules — a course that breaks all of them still
 * generates, still validates, and still plays. That is exactly why they need tests: nothing else
 * in the build would ever notice them quietly stopping, which is how the composer ended up
 * implementing three of the four structural steps and nobody spotting the missing one.
 */
class CourseLessonTest {

    private static final int SEEDS = 30;

    private static List<Segment> segmentsOf(CourseComposer.Composition c) {
        List<Segment> out = new ArrayList<>();
        for (String id : c.segmentIds()) {
            SegmentLibrary.all().stream()
                    .filter(seg -> seg.spec().id().equals(id))
                    .findFirst().ifPresent(out::add);
        }
        return out;
    }

    /** The lesson actually appears, and more than once — set up and then paid off. */
    @Test
    void theLessonIsTaughtAndThenTested() {
        List<String> failures = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (long seed = 0; seed < SEEDS; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, 360, 3, seed);
                long carrying = segmentsOf(c).stream()
                        .filter(seg -> seg.spec().tags().contains(c.lesson()))
                        .count();
                if (carrying < 2) {
                    failures.add(theme + " seed" + seed + " lesson=" + c.lesson()
                            + " appeared " + carrying + " time(s)");
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.size() + " courses failed to build on their own "
                + "lesson, e.g. " + failures.subList(0, Math.min(5, failures.size())));
    }

    /**
     * The hardest appearance of the lesson comes after the first one.
     *
     * <p>This is the "conclude" step stated as an assertion: teaching something and then testing it
     * is a different level from testing it and then teaching it, and only one of those is a level.
     */
    @Test
    void theHardestUseOfTheLessonIsNotTheFirstOne() {
        List<String> failures = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (long seed = 0; seed < SEEDS; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, 360, 4, seed);
                List<Segment> segments = segmentsOf(c);
                int firstIndex = -1;
                int hardestIndex = -1;
                int hardest = -1;
                for (int i = 0; i < segments.size(); i++) {
                    Segment.SegmentSpec spec = segments.get(i).spec();
                    if (!spec.tags().contains(c.lesson())) {
                        continue;
                    }
                    if (firstIndex < 0) {
                        firstIndex = i;
                    }
                    if (spec.difficulty() > hardest) {
                        hardest = spec.difficulty();
                        hardestIndex = i;
                    }
                }
                if (firstIndex >= 0 && hardestIndex < firstIndex) {
                    failures.add(theme + " seed" + seed + " lesson=" + c.lesson());
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.size() + " courses tested the lesson before "
                + "teaching it, e.g. " + failures.subList(0, Math.min(5, failures.size())));
    }

    /** A theme's chosen lesson is one its own rules allow. */
    @Test
    void aThemeOnlyTeachesWhatItIsFor() {
        for (CourseTheme theme : CourseTheme.values()) {
            List<Segment.Tag> allowed = CourseLesson.rules(theme).lessons();
            for (long seed = 0; seed < SEEDS; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, 240, 2, seed);
                assertTrue(allowed.contains(c.lesson()),
                        theme + " seed" + seed + " chose " + c.lesson() + ", not in " + allowed);
            }
        }
    }

    /**
     * A theme's avoided mechanics are rare, not merely discouraged.
     *
     * <p>Stated as a ratio rather than a ban. The generator must always be able to fill space, so
     * an absolute prohibition would risk courses it cannot complete; what matters is that a ghost
     * house is not full of moving platforms, not that it never has one.
     */
    @Test
    void aThemeMostlyAvoidsWhatItIsNotFor() {
        for (CourseTheme theme : CourseTheme.values()) {
            var avoided = CourseLesson.rules(theme).avoided();
            if (avoided.isEmpty()) {
                continue;
            }
            int total = 0;
            int offending = 0;
            for (long seed = 0; seed < SEEDS; seed++) {
                for (Segment seg : segmentsOf(CourseComposer.compose(theme, 360, 3, seed))) {
                    total++;
                    if (seg.spec().tags().stream().anyMatch(avoided::contains)) {
                        offending++;
                    }
                }
            }
            double share = offending / (double) Math.max(1, total);
            assertTrue(share < 0.30D, theme + " spent " + Math.round(share * 100)
                    + "% of its segments on mechanics it is supposed to avoid " + avoided);
        }
    }
}
