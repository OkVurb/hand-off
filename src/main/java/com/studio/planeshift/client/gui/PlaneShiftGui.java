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
}
