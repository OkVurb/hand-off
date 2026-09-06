package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Everything the generator spawns can be cleared away again.
 *
 * <p>{@code CourseWriter.clear} removes entities by looking for {@code GENERATED_TAG} before it
 * rebuilds. Anything spawned under a different tag therefore survives the rebuild and is joined by
 * a fresh copy of itself, so the population grows by one on every visit -- and it grows silently,
 * because a course with two Goombas where it should have one still looks and plays like a course.
 *
 * <p>This is a whole-class-of-bug test rather than a test of any one builder. It caught the Toad
 * House host, which was given a descriptive tag of its own that nothing ever read.
 */
class SpawnTaggingTest {

    @Test
    void everySegmentTagsItsSpawnsForCleanup() {
        List<String> offenders = new ArrayList<>();
        for (Segment segment : SegmentLibrary.all()) {
            offenders.addAll(untagged(segment));
        }
        for (Segment segment : SegmentLibrary.setPieces()) {
            offenders.addAll(untagged(segment));
        }
        assertEquals(List.of(), offenders,
                "these spawns cannot be cleared and will accumulate on every rebuild");
    }

    @Test
    void theToadHouseTagsItsHost() {
        List<String> offenders = new ArrayList<>();
        for (CourseCanvas.EntitySpawn spawn : ToadHouseRoom.build().entities()) {
            if (!SegmentLibrary.GENERATED_TAG.equals(spawn.tag())) {
                offenders.add("toad_house/" + spawn.type().getDescriptionId()
                        + " tagged " + spawn.tag());
            }
        }
        assertEquals(List.of(), offenders, "a Toad House would gain a Toad on every visit");
    }

    private static List<String> untagged(Segment segment) {
        CourseCanvas c = new CourseCanvas();
        GenContext ctx = new GenContext(CourseTheme.GRASS, 3, new java.util.Random(1L),
                GenContext.LANE_HALF_WIDTH);
        segment.build(c, 0, 0, ctx);
        List<String> bad = new ArrayList<>();
        for (CourseCanvas.EntitySpawn spawn : c.entities()) {
            if (!SegmentLibrary.GENERATED_TAG.equals(spawn.tag())) {
                bad.add(segment.spec().id() + "/" + spawn.type().getDescriptionId()
                        + " tagged " + spawn.tag());
            }
        }
        return bad;
    }
}
