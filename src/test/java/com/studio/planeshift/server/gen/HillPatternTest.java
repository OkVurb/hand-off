package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The background carries pattern and depth, not two flat layers.
 *
 * <p>§5.7. The horizon is the one piece of art on screen continuously, so a flat silhouette is what
 * the player looks at all level without being given anything to look at.
 *
 * <p>Both checks count blocks at a specific depth. A test that merely counted "is there a backdrop"
 * passed before this change and would pass after it, which is the shape of test this project has
 * already been caught writing three times.
 */
class HillPatternTest {

    private static final int LENGTH = 480;

    /** Themes that reach the skyline path at all; the rest get a back wall or nothing. */
    private static final CourseTheme[] OUTDOOR = {
        CourseTheme.GRASS, CourseTheme.DESERT, CourseTheme.SNOW, CourseTheme.WATER
    };

    private static int count(CourseCanvas canvas, Block block, int z) {
        int found = 0;
        for (int x = -8; x < LENGTH; x++) {
            for (int y = -8; y < 32; y++) {
                var state = canvas.get(x, y, z);
                if (state != null && state.is(block)) {
                    found++;
                }
            }
        }
        return found;
    }

    @Test
    @DisplayName("hills are striped, not flat silhouettes")
    void hillsCarryPattern() {
        for (CourseTheme theme : OUTDOOR) {
            int striped = 0;
            for (long seed = 0; seed < 6; seed++) {
                CourseCanvas canvas = CourseComposer.compose(theme, LENGTH, 2, seed).canvas();
                if (count(canvas, ModBlocks.COURSE_HEDGE_DISTANT_BAND.get(),
                        CourseDecorator.BACKDROP_Z) > 0) {
                    striped++;
                }
            }
            assertTrue(striped >= 4,
                    theme + ": only " + striped + " of 6 courses had any banding in the far layer, "
                            + "so its horizon is a flat shape");
        }
    }

    @Test
    @DisplayName("and a cloud bank sits between the terrain and them")
    void thereIsAMiddleDistance() {
        for (CourseTheme theme : OUTDOOR) {
            int banked = 0;
            for (long seed = 0; seed < 6; seed++) {
                CourseCanvas canvas = CourseComposer.compose(theme, LENGTH, 2, seed).canvas();
                if (count(canvas, ModBlocks.COURSE_CLOUD_BLOCK_FAR.get(), CourseDecorator.FAR_Z) > 0) {
                    banked++;
                }
            }
            assertTrue(banked >= 4,
                    theme + ": only " + banked + " of 6 courses had a cloud bank; without a middle "
                            + "distance the backdrop is two layers at no particular distance");
        }
    }

    /*
     * The sky theme is deliberately not asserted here.
     *
     * cloudBank() skips it, and the reason is good -- its ground is already cloud, so a bank of the
     * same material in front of it reads as more floor. But COURSE_CLOUD_BLOCK_FAR is also that
     * theme's own fill material, so "no bank in the sky" cannot be measured by looking for the
     * block: it is there either way, placed as terrain.
     *
     * The first version of this file asserted it anyway and went red, which is the test being wrong
     * rather than the code. Recorded rather than deleted quietly, and rather than loosened into
     * something that passes without meaning anything.
     */
}
