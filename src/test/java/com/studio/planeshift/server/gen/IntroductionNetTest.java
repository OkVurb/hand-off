package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The first gap a course shows the player is netted.
 *
 * <p>Composition-only, so it runs under plain JUnit with no server. The rule it pins is a design
 * rule rather than a correctness one — a course is perfectly valid without it, which is exactly why
 * it needs a test: nothing else would ever notice it quietly stopping.
 */
class IntroductionNetTest {

    private static final int SEEDS = 40;

    /** Every course that contains a gap segment marks where its introduction was netted. */
    @Test
    void everyCourseWithGapsNetsTheFirstOne() {
        List<String> failures = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (int difficulty = 1; difficulty <= 5; difficulty++) {
                for (long seed = 0; seed < SEEDS; seed++) {
                    CourseComposer.Composition c =
                            CourseComposer.compose(theme, 240, difficulty, seed);
                    boolean hasGapSegment = c.segmentIds().stream().anyMatch(
                            id -> SegmentLibrary.all().stream().anyMatch(
                                    seg -> seg.spec().id().equals(id)
                                            && seg.spec().tags().contains(Segment.Tag.GAP)));
                    boolean netted = c.canvas().markers().containsKey("intro_net");
                    if (hasGapSegment && !netted) {
                        failures.add(theme + " d" + difficulty + " seed" + seed);
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(),
                failures.size() + " courses had a gap segment and no net, e.g. "
                        + failures.subList(0, Math.min(5, failures.size())));
    }

    /**
     * No column of the introducing segment is bottomless.
     *
     * <p>This is the feature, stated directly. The marker records the floor the segment was built
     * on; the net sits {@link CourseComposer#INTRO_NET_DROP} below it, so every column across the
     * segment must have something solid within that reach — otherwise there is still a hole in the
     * one place the course has promised there is not.
     */
    @Test
    void theFirstGapHasNoBottomlessColumn() {
        List<String> failures = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (long seed = 0; seed < SEEDS; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, 240, 3, seed);
                BlockPos net = c.canvas().markers().get("intro_net");
                if (net == null) {
                    continue;
                }
                // The narrowest gap segment in the catalogue is twelve wide, so this stays inside
                // the segment that was netted without needing its width recorded.
                for (int x = net.getX(); x < net.getX() + 12; x++) {
                    boolean floored = false;
                    for (int drop = 0; drop <= CourseComposer.INTRO_NET_DROP; drop++) {
                        if (c.canvas().get(x, net.getY() - drop, 0) != null) {
                            floored = true;
                            break;
                        }
                    }
                    if (!floored) {
                        failures.add(theme + " seed" + seed + " column x=" + x);
                        break;
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(),
                failures.size() + " introducing segments still had a bottomless column, e.g. "
                        + failures.subList(0, Math.min(5, failures.size())));
    }

}
