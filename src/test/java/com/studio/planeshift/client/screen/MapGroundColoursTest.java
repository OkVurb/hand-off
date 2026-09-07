package com.studio.planeshift.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Each world's map has to look like its own world.
 *
 * <p>The map background was one hardcoded grassy gradient for every world, volcano and ice
 * included. Nothing in the suite could notice, because rendering is not tested and a screen that
 * draws the wrong colour draws it perfectly happily.
 *
 * <p>These are the two claims worth pinning that do not require rendering anything: the palettes
 * are actually distinct, and every theme has one. The look of the result still needs eyes.
 */
class MapGroundColoursTest {

    @Test
    @DisplayName("no two themes share a ground palette")
    void palettesAreDistinct() {
        Set<Long> seen = new HashSet<>();
        for (CourseTheme theme : CourseTheme.values()) {
            long pair = ((long) CourseMapScreen.groundTop(theme) << 32)
                    | (CourseMapScreen.groundBottom(theme) & 0xFFFFFFFFL);
            assertTrue(seen.add(pair),
                    theme + " reuses another theme's ground palette, so its map is not its own");
        }
        assertEquals(CourseTheme.values().length, seen.size());
    }

    @Test
    @DisplayName("every theme darkens toward the bottom, so the field has a horizon")
    void gradientsDescend() {
        for (CourseTheme theme : CourseTheme.values()) {
            int top = CourseMapScreen.groundTop(theme);
            int bottom = CourseMapScreen.groundBottom(theme);
            assertNotEquals(top, bottom, theme + " has a flat gradient");
            assertTrue(luminance(bottom) < luminance(top),
                    theme + " gets lighter toward the bottom, which reads as a ceiling");
        }
    }

    private static int luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 30 + g * 59 + b * 11) / 100;
    }
}
