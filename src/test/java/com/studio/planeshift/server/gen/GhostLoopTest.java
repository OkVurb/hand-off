package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.block.LoopTriggerBlock;
import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import org.junit.jupiter.api.Test;

/**
 * The endless hall loops, and does not trap anyone.
 *
 * <p>LoopTriggerBlock teleports whatever touches it 25 blocks backwards. That is a soft-lock on any
 * route the player has to get past, so every one of these tests is really the same question asked
 * from a different side: is the loop optional, and can you get out of it?
 */
class GhostLoopTest {

    private static CourseCanvas build() {
        CourseCanvas c = new CourseCanvas();
        GenContext ctx = new GenContext(CourseTheme.GHOST_HOUSE, 3, new java.util.Random(1L),
                GenContext.LANE_HALF_WIDTH);
        SegmentLibrary.GHOST_LOOP.build(c, 0, 0, ctx);
        return c;
    }

    @Test
    void theTeleportLandsOnTheBalconyRatherThanInOpenAir() {
        // The one arithmetic fact the whole segment rests on. If LOOP_DISTANCE is ever retuned,
        // this fails rather than the player quietly being dropped down a hole.
        assertEquals(SegmentLibrary.LOOP_RETURN_X,
                SegmentLibrary.LOOP_TRIGGER_X - LoopTriggerBlock.LOOP_DISTANCE,
                "the trigger no longer returns the player to the start of the balcony");

        CourseCanvas c = build();
        assertTrue(c.blocks().containsKey(
                        CourseCanvas.key(SegmentLibrary.LOOP_RETURN_X, 5, 0)),
                "nothing to land on at the return column");
    }

    @Test
    void theBalconyIsSemisolidSoItIsNotACeilingOverTheMainRoute() {
        CourseCanvas c = build();
        for (int i = SegmentLibrary.LOOP_RETURN_X; i <= SegmentLibrary.LOOP_TRIGGER_X; i++) {
            assertTrue(c.blocks().get(CourseCanvas.key(i, 5, 0))
                            .is(ModBlocks.SEMISOLID_PLATFORM.get()),
                    "balcony column " + i + " is solid, and so roofs the low road");
        }
    }

    @Test
    void theFloorUnderneathIsUnbrokenSoTheLoopIsAlwaysOptional() {
        CourseCanvas c = build();
        for (int i = 0; i < 30; i++) {
            assertTrue(c.blocks().containsKey(CourseCanvas.key(i, 0, 0)),
                    "gap in the main floor at " + i + ": the loop stops being optional");
        }
    }

    @Test
    void theTriggerIsOnlyReachableFromTheBalcony() {
        CourseCanvas c = build();
        // Standing on the floor, the player occupies y+1 and y+2. A trigger down there would fire
        // on someone simply walking the main route, which is the soft-lock this layout avoids.
        for (int y = 1; y <= 4; y++) {
            assertFalse(hasLoopAt(c, SegmentLibrary.LOOP_TRIGGER_X, y),
                    "loop trigger at head height on the main floor (y=" + y + ")");
        }
        assertTrue(hasLoopAt(c, SegmentLibrary.LOOP_TRIGGER_X, 6)
                        && hasLoopAt(c, SegmentLibrary.LOOP_TRIGGER_X, 7),
                "trigger does not cover both foot and head height on the balcony");
    }

    @Test
    void thereAreStairsAtTheReturnEndSoTheLoopCanBeLeft() {
        CourseCanvas c = build();
        // Without these the player is returned to a balcony they cannot get off, and the only way
        // out is dying. The stairs are the difference between a joke and a trap.
        for (int step = 0; step < 3; step++) {
            assertTrue(c.blocks().containsKey(CourseCanvas.key(1 + step, 2 + step, 0)),
                    "missing stair at step " + step);
        }
    }

    private static boolean hasLoopAt(CourseCanvas c, int x, int y) {
        var state = c.blocks().get(CourseCanvas.key(x, y, 0));
        return state != null && state.is(ModBlocks.LOOP_TRIGGER.get());
    }
}
