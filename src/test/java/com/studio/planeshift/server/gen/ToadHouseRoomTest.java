package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.ToadHouseGifts;
import com.studio.planeshift.common.registry.ModBlocks;
import org.junit.jupiter.api.Test;

/**
 * The Toad House is a room you can stand in and reach the boxes from.
 *
 * <p>It used to be built by the ordinary composer, so it was a 64-block grass course with a
 * flagpole -- valid, traversable, and not a Toad House. None of the generator tests could have
 * caught that, because there was nothing wrong with it as a course.
 */
class ToadHouseRoomTest {

    @Test
    void thereAreExactlyThreeBoxes() {
        // Three is the mechanic. One box is a vending machine and four is a shopping trip.
        assertEquals(3, count(ModBlocks.TOAD_BOX.get()),
                "a Toad House is a choice between three boxes");
    }

    @Test
    void theBoxesAreWithinJumpingReach() {
        CourseCanvas c = ToadHouseRoom.build();
        // The floor fills y=0, so the player stands at y=1. A box more than three clear blocks
        // overhead cannot be headbutted, and the room contains nothing to climb.
        c.blocks().forEach((key, state) -> {
            if (state.is(ModBlocks.TOAD_BOX.get())) {
                int y = CourseCanvas.unpackY(key);
                assertTrue(y >= 3 && y <= 4, "box at y=" + y + " is out of jumping reach");
            }
        });
    }

    @Test
    void theRoomIsEnclosedButOpenToTheCamera() {
        CourseCanvas c = ToadHouseRoom.build();
        int half = GenContext.LANE_HALF_WIDTH;
        // Floor and ceiling over the lane, and a wall behind it...
        assertTrue(c.blocks().containsKey(CourseCanvas.key(10, 0, 0)), "no floor");
        assertTrue(c.blocks().containsKey(CourseCanvas.key(10, 8, 0)), "no ceiling");
        assertTrue(c.blocks().containsKey(CourseCanvas.key(10, 3, half + 1)), "no back wall");
        // ...and nothing on the side the player looks in from. Walling this would hide the room
        // from the only angle it is ever seen at.
        assertTrue(c.blocks().get(CourseCanvas.key(10, 3, -half - 1)) == null,
                "the camera side is walled off");
    }

    @Test
    void thereIsSomethingToLightIt() {
        assertTrue(count(ModBlocks.COURSE_LAMP.get()) > 0,
                "a sealed room with no lamp is a dark room, and the boxes are the point of it");
    }

    @Test
    void everyGiftIsWorthCrossingTheMapFor() {
        // The list is shared with MapNodeService's fallback precisely so the two cannot drift.
        assertTrue(ToadHouseGifts.size() >= 4, "too few gifts for the choice to mean anything");
    }

    private static long count(net.minecraft.world.level.block.Block block) {
        return ToadHouseRoom.build().blocks().values().stream()
                .filter(state -> state.is(block))
                .count();
    }
}
