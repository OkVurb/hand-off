package com.studio.planeshift.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The card a course opens on.
 *
 * <p>Two lines over a darkened screen: the world's name, and the course's number under it. Held,
 * then faded. The reference does this before every level and it is doing two jobs at once -- it
 * says where you are in the run, and it gives the player a beat to settle before the clock starts.
 *
 * <p>Drawn as an overlay rather than as a {@code Screen}, deliberately. A screen would pause input
 * and release the mouse, which turns a half-second flourish into a dialog the player has to
 * dismiss; the reference's card never stops the game either, it just sits over the first moments
 * of it.
 *
 * <p>Client-only and entirely presentational, like {@link ScorePopups}. Nothing waits on it.
 */
public final class TitleCard {

    /** How long the card is fully opaque. */
    private static final long HOLD_MS = 1100L;

    /** How long it takes to fade out afterwards. */
    private static final long FADE_MS = 700L;

    private static final int WORLD_COLOUR = 0xFFFFFFFF;
    private static final int LEVEL_COLOUR = 0xFFFFE066;

    private static Component world;
    private static Component level;
    private static long shownAtMs;

    private TitleCard() {
    }

    /** Shows the card. A second call restarts it rather than queueing. */
    public static void show(String worldName, String levelName) {
        world = Component.literal(worldName);
        level = Component.literal(levelName);
        shownAtMs = System.currentTimeMillis();
    }

    /** Clears the card early, for a player who leaves the course before it has faded. */
    public static void clear() {
        world = null;
        level = null;
    }

    public static void render(GuiGraphics graphics, Font font) {
        if (world == null) {
            return;
        }
        long age = System.currentTimeMillis() - shownAtMs;
        if (age > HOLD_MS + FADE_MS) {
            clear();
            return;
        }
        float alpha = age <= HOLD_MS ? 1.0F : 1.0F - (age - HOLD_MS) / (float) FADE_MS;
        int fade = (int) (alpha * 255.0F) << 24;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // A band rather than a full-screen wash: the player can still see the level they are
        // standing in, which is the difference between a title card and a loading screen.
        int bandTop = height / 2 - 26;
        graphics.fill(0, bandTop, width, bandTop + 52, (int) (alpha * 165.0F) << 24);

        int midY = height / 2;
        graphics.drawCenteredString(font, level, width / 2, midY - 16, LEVEL_COLOUR & 0xFFFFFF | fade);
        graphics.drawCenteredString(font, world, width / 2, midY + 2, WORLD_COLOUR & 0xFFFFFF | fade);
    }
}
