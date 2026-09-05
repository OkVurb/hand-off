package com.studio.planeshift.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Shared PlaneShift GUI theme: bright sky, ground stripe, brick border, and rounded panels.
 */
public final class PlaneShiftGui {

    public static final int SKY_TOP = 0xFF_2080FF;
    public static final int SKY_BOTTOM = 0xFF_70C0FF;
    public static final int GROUND_TOP = 0xFF_44AA22;
    public static final int GROUND_BOTTOM = 0xFF_6B4226;
    public static final int BRICK = 0xFF_CC5533;
    public static final int MORTAR = 0xFF_4A2A1A;
    public static final int PANEL = 0xD0_1A2A3A;
    public static final int PANEL_ACCENT = 0xFF_FFCF00;
    public static final int TEXT_TITLE = 0xFF_FFFFFF;
    public static final int TEXT_SHADOW = 0xFF_000000;
    public static final int COIN_YELLOW = 0xFF_FFD700;

    private PlaneShiftGui() {
    }

    /** Draws the Mario-style world-map background behind a full screen. */
    public static void renderThemedBackground(GuiGraphics graphics, int width, int height) {
        // Sky gradient.
        graphics.fillGradient(0, 0, width, height - 40, SKY_TOP, SKY_BOTTOM);

        // Ground band.
        int groundY = height - 40;
        graphics.fillGradient(0, groundY, width, height, GROUND_TOP, GROUND_BOTTOM);
        // Grass top edge.
        graphics.fill(0, groundY, width, groundY + 4, 0xFF_55CC22);

        // Brick border at the top.
        int brickHeight = 24;
        for (int x = 0; x < width; x += 24) {
            graphics.fill(x, 0, Math.min(x + 22, width), brickHeight, BRICK);
            graphics.hLine(x, Math.min(x + 22, width), 0, MORTAR);
            graphics.hLine(x, Math.min(x + 22, width), brickHeight - 1, MORTAR);
            graphics.vLine(x, 0, brickHeight, MORTAR);
            graphics.vLine(Math.min(x + 22, width), 0, brickHeight, MORTAR);
        }

        // A few simple clouds.
        drawCloud(graphics, width / 6, height / 6, 2);
        drawCloud(graphics, width * 3 / 4, height / 5, 1);
        drawCloud(graphics, width * 2 / 3, height / 3, 3);

        // Question-block accents along the bottom ground.
        for (int x = 16; x < width; x += 120) {
            drawQuestionBlock(graphics, x, height - 30, 18);
        }
    }

    /** Draws a soft cloud made of rounded white puffs. */
    public static void drawCloud(GuiGraphics graphics, int x, int y, int scale) {
        int color = 0xD0_FFFFFF;
        int s = 6 * scale;
        graphics.fill(x, y + s, x + s * 5, y + s * 2, color);
        graphics.fill(x + s, y, x + s * 4, y + s * 3, color);
        graphics.fill(x + s * 2, y - s / 2, x + s * 3, y + s * 3, color);
    }

    /** Draws a tiny question block accent. */
    public static void drawQuestionBlock(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF_FFCF00);
        graphics.hLine(x, x + size, y, 0xFF_AA8800);
        graphics.hLine(x, x + size, y + size - 1, 0xFF_AA8800);
        graphics.vLine(x, y, y + size, 0xFF_AA8800);
        graphics.vLine(x + size - 1, y, y + size, 0xFF_AA8800);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            graphics.drawCenteredString(minecraft.font, "?",
                    x + size / 2, y + size / 2 - 4, 0xFF_AA4400);
        }
    }

    /** Draws a rounded-ish dark panel with a gold border. */
    public static void renderPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, PANEL_ACCENT);
        graphics.fill(x, y, x + w, y + h, PANEL);
    }

    /** Draws a large title with a subtle shadow. */
    public static void drawTitle(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                 Component title, int x, int y, int color) {
        graphics.drawString(font, title, x + 1, y + 1, TEXT_SHADOW, false);
        graphics.drawString(font, title, x, y, color, false);
    }

    /** Creates a themed button. */
    public static Button themedButton(Component label, int x, int y, int w, int h, Button.OnPress onPress) {
        return Button.builder(label, onPress)
                .pos(x, y)
                .size(w, h)
                .build();
    }

    /** Mixes a packed ARGB colour toward white. Used by the menu buttons for their lit edges. */
    public static int lighten(int argb, float amount) {
        return mix(argb, 0xFFFFFF, amount);
    }

    /** Mixes a packed ARGB colour toward black. */
    public static int darken(int argb, float amount) {
        return mix(argb, 0x000000, amount);
    }

    private static int mix(int argb, int target, float amount) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int tr = (target >> 16) & 0xFF;
        int tg = (target >> 8) & 0xFF;
        int tb = target & 0xFF;
        r = Math.round(r + (tr - r) * amount);
        g = Math.round(g + (tg - g) * amount);
        b = Math.round(b + (tb - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * A bright parallax sky: gradient, drifting cloud banks, and rolling hills.
     *
     * <p>Replaces a static backdrop. A menu that does not move reads as a paused game, and the
     * modern Mario front ends all keep something drifting behind the buttons for exactly that
     * reason - it costs nothing and it is the difference between a screen and a place.
     *
     * @param time a monotonically increasing tick count; the drift is derived from it
     */
    public static void renderParallaxSky(GuiGraphics graphics, int width, int height, float time) {
        for (int y = 0; y < height; y++) {
            float t = y / (float) Math.max(1, height);
            graphics.fill(0, y, width, y + 1, mix(SKY_TOP, SKY_BOTTOM & 0xFFFFFF, t));
        }

        // Two cloud banks at different speeds. Parallax is the whole trick: one layer moving is a
        // slideshow, two at different rates is depth.
        drawCloudBank(graphics, width, (int) (height * 0.18F), time * 0.25F, 3, 0x50FFFFFF);
        drawCloudBank(graphics, width, (int) (height * 0.30F), time * 0.55F, 4, 0x90FFFFFF);

        int horizon = height - 64;
        for (int i = 0; i < 5; i++) {
            int hx = (int) ((i * 150 - time * 0.15F) % (width + 300)) - 150;
            int r = 70 + (i % 3) * 26;
            graphics.fill(hx - r, horizon - r / 3, hx + r, horizon, 0xFF3E8E3A);
        }
        graphics.fill(0, horizon, width, horizon + 6, 0xFF6ECB4A);
        graphics.fill(0, horizon + 6, width, height, 0xFF7A5230);
    }

    private static void drawCloudBank(GuiGraphics graphics, int width, int y, float drift,
                                      int scale, int colour) {
        int span = 220;
        for (int i = -1; i < width / span + 2; i++) {
            int x = (int) ((i * span - drift) % (width + span * 2)) - span;
            int s = 4 * scale;
            graphics.fill(x, y, x + s * 5, y + s, colour);
            graphics.fill(x + s, y - s, x + s * 4, y, colour);
            graphics.fill(x + s * 2, y - s * 2, x + s * 3, y - s, colour);
        }
    }

}
