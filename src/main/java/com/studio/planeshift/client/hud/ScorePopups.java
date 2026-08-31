package com.studio.planeshift.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Floating score numbers for the stomp combo ladder.
 *
 * <p>Each defeated enemy pushes the points it earned, and the popups rise and fade so a chain
 * reads as "+100, +200, +400" climbing rather than as a HUD counter jumping by an amount the
 * player has to work out.
 *
 * <p>Anchored to the HUD rather than to the enemy in world space. The 1.21.11 level-render events
 * no longer hand out a camera or partial tick, so world-anchoring would mean reconstructing the
 * projection by hand for a purely cosmetic label. Screen-anchored popups convey the same
 * information — which rung of the ladder just paid — without depending on render internals that
 * have changed twice in recent versions.
 *
 * <p>Client-only and entirely presentational: the authoritative score lives in
 * {@code CourseState} and arrives with the rest of the synced state.
 */
public final class ScorePopups {

    /** How long a popup stays on screen. */
    private static final long LIFETIME_MS = 1200L;
    /** How far it drifts upward over its life, in pixels. */
    private static final int RISE_PIXELS = 22;
    /** Most popups kept at once; a long chain should not fill the screen. */
    private static final int MAX_POPUPS = 6;

    private static final int POINTS_COLOUR = 0xFFFFE066;
    private static final int ONE_UP_COLOUR = 0xFF66FF88;

    private record Popup(Component text, int colour, long bornMs) {
    }

    private static final Deque<Popup> POPUPS = new ArrayDeque<>();

    private ScorePopups() {
    }

    /**
     * Queues a popup. {@code amount} of zero means the chain paid out a 1-Up instead of points,
     * which gets its own wording and colour because it is the more important event.
     */
    public static void add(int amount) {
        Component text = amount > 0
                ? Component.literal("+" + amount)
                : Component.translatable("hud.planeshift.one_up");
        int colour = amount > 0 ? POINTS_COLOUR : ONE_UP_COLOUR;

        POPUPS.addLast(new Popup(text, colour, System.currentTimeMillis()));
        while (POPUPS.size() > MAX_POPUPS) {
            POPUPS.removeFirst();
        }
    }

    /** Drops everything, for leaving a course or disconnecting. */
    public static void clear() {
        POPUPS.clear();
    }

    /** Draws the live popups. Called from {@link CourseHud}. */
    public static void render(GuiGraphics graphics, Font font) {
        if (POPUPS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        POPUPS.removeIf(p -> now - p.bornMs() > LIFETIME_MS);

        int centreX = graphics.guiWidth() / 2;
        int baseY = graphics.guiHeight() / 2 - 20;
        int slot = 0;

        for (Popup popup : POPUPS) {
            float age = (now - popup.bornMs()) / (float) LIFETIME_MS;
            int alpha = (int) (255 * (1.0F - age * age));   // hold, then fade off quickly
            if (alpha <= 8) {
                slot++;
                continue;
            }
            int y = baseY - (int) (RISE_PIXELS * age) - slot * 11;
            int x = centreX - font.width(popup.text()) / 2;
            graphics.drawString(font, popup.text(), x, y,
                    (alpha << 24) | (popup.colour() & 0x00FFFFFF));
            slot++;
        }
    }

    /** True when the local player is not in a course, so the HUD can drop stale popups. */
    static boolean shouldClear(Minecraft minecraft) {
        return minecraft.player == null;
    }
}
