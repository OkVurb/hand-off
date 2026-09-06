package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every theme actually gets a climax.
 *
 * <p>The composer reserves the window between 62% and 82% of a course for a set piece. For most of
 * this project CASTLE_BRIDGE was the only one in the library and it is restricted to lava and
 * underground, so four of the six themes reserved that slot and silently filled it with ordinary
 * segments. Nothing failed: a course without a set piece is a perfectly valid course, just a
 * flatter one, which is exactly why no existing test noticed.
 *
 * <p>So this asserts the thing that was actually wrong -- not that the segments exist, but that
 * they reach a player. A set piece that never passes the teaching gate is no better than one that
 * was never written.
 */
class SetPieceCoverageTest {

    private static final int SEEDS = 60;
    private static final int LENGTH = 480;

    @Test
    void everyThemeReachesASetPiece() {
        List<String> barren = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            int withSetPiece = 0;
            for (long seed = 0; seed < SEEDS; seed++) {
                CourseComposer.Composition c =
                        CourseComposer.compose(theme, LENGTH, 4, seed);
                if (c.segmentIds().stream().anyMatch(SetPieceCoverageTest::isSetPiece)) {
                    withSetPiece++;
                }
            }
            if (withSetPiece == 0) {
                barren.add(theme.name());
            }
        }
        assertTrue(barren.isEmpty(),
                "these themes never build to anything: " + barren);
    }

    @Test
    void aSetPieceIsStillOccasionalRatherThanGuaranteed() {
        // The window is a window, not a slot that must be filled. If every course of every theme
        // ended the same way the set piece would stop being a set piece and become the third act.
        int total = 0;
        int withSetPiece = 0;
        for (CourseTheme theme : CourseTheme.values()) {
            for (long seed = 0; seed < SEEDS; seed++) {
                total++;
                CourseComposer.Composition c = CourseComposer.compose(theme, LENGTH, 4, seed);
                if (c.segmentIds().stream().anyMatch(SetPieceCoverageTest::isSetPiece)) {
                    withSetPiece++;
                }
            }
        }
        assertTrue(withSetPiece < total,
                "every single course now ends in a set piece, which makes it not one");
    }

    private static boolean isSetPiece(String id) {
        for (Segment segment : SegmentLibrary.setPieces()) {
            if (segment.spec().id().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
