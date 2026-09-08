package com.studio.planeshift.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The circle that opens onto a course.
 *
 * <p>The reference joins its map and its levels with an iris: the screen closes to a point on the
 * node just cleared, and opens from one onto the level. It is doing real work rather than
 * decoration -- it hides the moment of loading, and it tells the player that the two screens are
 * the same place seen at different scales, which is the whole conceit of a world map.
 *
 * <p>Drawn as scanlines rather than as a mask texture. For each row of the screen the circle has a
 * known half-width, so the black outside it is two rectangles; the cost is one fill per row and it
 * needs no texture, no shader and no blend state. A circular mask cut in a PNG would have to be
 * authored at one size and stretched, and the one thing an iris cannot survive is being an oval.
 *
 * <p>Paired with {@link TitleCard} and, like it, purely presentational: it draws over the game and
 * never stops it, so a player who knows the level can already be running while it opens.
 */
public final class IrisWipe {

    /** How long the circle takes to open. */
    private static final long OPEN_MS = 620L;

    private static long startedAtMs;
    private static boolean running;
    private static boolean closing;

    private IrisWipe() {
    }

    /** Starts the iris opening. A second call restarts it rather than queueing. */
    public static void open() {
        startedAtMs = System.currentTimeMillis();
        running = true;
        closing = false;
    }

    /**
     * Starts the iris closing, for the end of a course.
     *
     * <p>The same circle run backwards, and it needed a different home rather than a different
     * class: the results screen is a {@code Screen}, and the HUD does not draw underneath one. So
     * this is called by the screen itself, which is why {@link #render} is public and stateless
     * about who is calling it.
     */
    public static void close() {
        startedAtMs = System.currentTimeMillis();
        running = true;
        closing = true;
    }

    /** Cuts it short, for a player who leaves before it finishes. */
    public static void clear() {
        running = false;
    }

    public static void render(GuiGraphics graphics) {
        if (!running) {
            return;
        }
        long age = System.currentTimeMillis() - startedAtMs;
        if (age >= OPEN_MS) {
            running = false;
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int cx = width / 2;
        int cy = height / 2;
        // Eased rather than linear. A circle growing at a constant rate covers area at an
        // accelerating rate, so it reads as speeding up; easing out makes the opening feel even.
        float t = age / (float) OPEN_MS;
        float eased = 1.0F - (1.0F - t) * (1.0F - t);
        if (closing) {
            // Run backwards, and eased the other way round so the ease is still on the slow end of
            // the movement. Mirroring the curve as well as the direction would put the slow part
            // at the start of a close, which reads as hesitation rather than as an ending.
            eased = 1.0F - t * t;
        }
        // The final radius has to clear the corners, not the edges, or the last thing the player
        // sees is four black triangles.
        double full = Math.sqrt(cx * cx + cy * cy);
        int radius = (int) (eased * full);

        for (int y = 0; y < height; y++) {
            int dy = y - cy;
            int half = radius * radius - dy * dy;
            if (half <= 0) {
                graphics.fill(0, y, width, y + 1, 0xFF000000);
                continue;
            }
            int span = (int) Math.sqrt(half);
            graphics.fill(0, y, cx - span, y + 1, 0xFF000000);
            graphics.fill(cx + span, y, width, y + 1, 0xFF000000);
        }
    }
}
