package com.studio.planeshift.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The credits, rolling over a level the player is still holding the controls of.
 *
 * <p>§7.8, and the half that had been recorded as unbuilt. The reference rolls its credits over a
 * playable stage rather than over a black screen, and the difference is the whole point: a credits
 * screen is something the player waits out, and a credits <em>level</em> is a victory lap they are
 * still driving. Nothing here locks input, pauses the world or opens a {@code Screen} — this is an
 * overlay on the ordinary HUD, drawn over an ordinary course.
 *
 * <p>The names live here rather than arriving in the packet. They are the same every run, so
 * sending them would be sending a constant down a wire; what the server knows and the client does
 * not is when to start.
 */
public final class CreditsRoll {

    /** How long one line takes to cross the screen. */
    private static final long LINE_MS = 2600L;

    /** How far apart the lines are, as a fraction of a line's travel. */
    private static final double SPACING = 0.55D;

    /**
     * The roll.
     *
     * <p>Credits for a mod built by one person and several agents, which is unusual enough to be
     * worth stating plainly rather than dressing up as a studio.
     */
    private static final String[] LINES = {
        "PlaneShift",
        "",
        "A Mario-style platformer",
        "built inside Minecraft",
        "",
        "Design and direction",
        "OkVurb",
        "",
        "Reference study",
        "Four hours of footage,",
        "read frame by frame",
        "",
        "Built with",
        "Claude, Codex and Devin",
        "",
        "Thanks for playing",
    };

    private static long startedAtMs;
    private static boolean running;

    private CreditsRoll() {
    }

    /** Starts the roll. */
    public static void start() {
        startedAtMs = System.currentTimeMillis();
        running = true;
    }

    /** Stops it early, for a player who leaves the course. */
    public static void clear() {
        running = false;
    }

    public static boolean running() {
        return running;
    }

    public static void render(GuiGraphics graphics, Font font) {
        if (!running) {
            return;
        }
        long age = System.currentTimeMillis() - startedAtMs;
        long total = (long) (LINE_MS * (1.0D + LINES.length * SPACING));
        if (age > total) {
            running = false;
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int i = 0; i < LINES.length; i++) {
            if (LINES[i].isEmpty()) {
                continue;
            }
            long offset = (long) (i * LINE_MS * SPACING);
            long own = age - offset;
            if (own < 0 || own > LINE_MS) {
                continue;
            }
            // Bottom to top, and fading at both ends so a line does not pop into existence at the
            // edge of the screen -- which at this speed reads as a glitch rather than as a roll.
            float t = own / (float) LINE_MS;
            int y = (int) (height - t * (height + 20)) + 10;
            float alpha = Math.min(1.0F, Math.min(t, 1.0F - t) * 6.0F);
            int colour = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
            graphics.drawCenteredString(font, Component.literal(LINES[i]), width / 2, y, colour);
        }
    }
}
