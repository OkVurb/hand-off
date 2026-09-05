package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

/**
 * Blocks the generator is supposed to place actually get placed.
 *
 * <p>This test exists because an audit found nine registered blocks — ON/OFF blocks and their
 * switch, coin rings, music blocks, vines, tiles, banners, the shift gate — that were fully built,
 * textured, modelled, and never appeared in a single generated course. They were real to a player
 * browsing the creative menu and imaginary to a player playing the game.
 *
 * <p>Nothing else would ever have caught that. Every other generator test asks whether a course is
 * valid, and a course containing none of these is perfectly valid; it is just missing most of what
 * was built for it.
 */
class BlockCoverageTest {

    private static final int SEEDS = 40;

    /** Blocks that exist to appear in courses, and so must appear in some course. */
    private static List<Block> expected() {
        return List.of(
                ModBlocks.ON_OFF_BLOCK.get(),
                ModBlocks.ON_OFF_SWITCH.get(),
                ModBlocks.COIN_RING_BLOCK.get(),
                ModBlocks.MUSIC_BLOCK.get(),
                ModBlocks.COURSE_VINE.get(),
                ModBlocks.COURSE_TILE.get(),
                ModBlocks.COURSE_BANNER.get(),
                ModBlocks.SHIFT_GATE.get());
    }

    @Test
    void everyCourseBlockAppearsInSomeGeneratedCourse() {
        Set<Block> seen = new HashSet<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (int difficulty = 1; difficulty <= 5; difficulty++) {
                for (long seed = 0; seed < SEEDS; seed++) {
                    CourseComposer.Composition c =
                            CourseComposer.compose(theme, 360, difficulty, seed);
                    for (BlockState state : c.canvas().blocks().values()) {
                        seen.add(state.getBlock());
                    }
                }
            }
        }

        List<String> missing = new ArrayList<>();
        for (Block block : expected()) {
            if (!seen.contains(block)) {
                missing.add(block.getDescriptionId());
            }
        }
        assertTrue(missing.isEmpty(),
                "registered, textured, and never generated: " + missing);
    }
}
