package com.studio.planeshift.client.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The iris has to actually reach the corners.
 *
 * <p>There is exactly one way this effect fails visibly and permanently: the radius is too small,
 * the animation completes, drawing stops, and a black wedge is left in whichever corner the circle
 * never covered. It would not throw and no rendering test would catch it -- the screen would simply
 * have a dark corner from then on.
 *
 * <p>So this checks the property rather than the picture: from any focus point, at full radius,
 * every corner is inside the circle.
 */
class IrisGeometryTest {

    private static void assertCoversCorners(int cx, int cy, int w, int h) {
        double r = CourseMapScreen.irisFullRadius(cx, cy, w, h);
        int[][] corners = {{0, 0}, {w, 0}, {0, h}, {w, h}};
        for (int[] c : corners) {
            double d = Math.hypot(c[0] - cx, c[1] - cy);
            assertTrue(d <= r + 1e-6,
                    "corner (" + c[0] + "," + c[1] + ") is " + d + " away but the iris stops at "
                            + r + " from focus (" + cx + "," + cy + ") on a " + w + "x" + h
                            + " screen");
        }
    }

    @Test
    @DisplayName("every corner is covered from a focus anywhere on screen")
    void coversFromAnywhere() {
        int[][] sizes = {{854, 480}, {1920, 1080}, {640, 360}, {1024, 768}};
        for (int[] size : sizes) {
            int w = size[0];
            int h = size[1];
            // Corners, edges, centre, and a scatter across the interior.
            for (int x = 0; x <= w; x += Math.max(1, w / 7)) {
                for (int y = 0; y <= h; y += Math.max(1, h / 7)) {
                    assertCoversCorners(x, y, w, h);
                }
            }
        }
    }

    @Test
    @DisplayName("a focus outside the screen still covers it")
    void coversFromOffScreen() {
        // Defensive: selected is clamped to a valid node, but a node at the very edge of the map
        // area plus a small window can put the focus outside the drawn region.
        assertCoversCorners(-40, -25, 854, 480);
        assertCoversCorners(900, 500, 854, 480);
    }
}
