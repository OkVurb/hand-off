package com.studio.planeshift.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Black bars, top and bottom, for the length of a cutscene.
 *
 * <p>This is the mod's one "you are watching now" signal, and it exists so the others can mean
 * their opposite. The title card, the iris and the credits all leave the player holding the
 * controls on purpose; without something that visibly does not, a player has no way to tell a
 * flourish from a scene.
 *
 * <p>The bars slide rather than appear. A cut to letterbox reads as the game breaking; a quarter
 * second of travel reads as a camera.
 */
public final class Letterbox {

    /** How tall the bars are at full extent, as a fraction of screen height. */
    private static final float DEPTH = 0.12F;

    /** Ticks the bars take to slide in, and out again. */
    private static final int TRAVEL = 6;

    private static int remaining;
    private static int total;

    private Letterbox() {
    }

    /** Lowers the bars for {@code ticks}, or raises them now if given zero. */
    public static void show(int ticks) {
        remaining = Math.max(0, ticks);
        total = remaining;
    }

    /** True while a scene is playing, so other overlays can stay out of its way. */
    public static boolean active() {
        return remaining > 0;
    }

    /** Advances the bars. Driven by the client tick so it keeps time with the server's script. */
    public static void tick() {
        if (remaining > 0) {
            remaining--;
        }
    }

    public static void render(GuiGraphics graphics) {
        if (remaining <= 0) {
            return;
        }
        int height = graphics.guiHeight();
        int width = graphics.guiWidth();
        int full = (int) (height * DEPTH);

        // Travel in at the start and out at the end, from the same number, so a scene shorter than
        // twice the travel still opens and closes rather than snapping.
        int elapsed = total - remaining;
        float in = Math.min(1.0F, elapsed / (float) TRAVEL);
        float out = Math.min(1.0F, remaining / (float) TRAVEL);
        int depth = (int) (full * Math.min(in, out));
        if (depth <= 0) {
            return;
        }
        graphics.fill(0, 0, width, depth, 0xFF000000);
        graphics.fill(0, height - depth, width, height, 0xFF000000);
    }
}
