package com.studio.planeshift.client.gui;

import com.studio.planeshift.client.screen.CourseMapScreen;
import com.studio.planeshift.common.network.CourseSelectPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The front door.
 *
 * <p>Rebuilt in the shape of the modern New Super Mario Bros. menus rather than the flat 8-bit look
 * it had, because that is the era this mod is actually imitating: a bright parallax sky, one
 * saturated colour per action, large soft targets, and a heavy outlined wordmark.
 *
 * <p>The previous version was four 200x20 vanilla buttons on a static backdrop. Nothing about it
 * was broken and all of it was wrong — the first screen decides what kind of game the player thinks
 * they have opened, and a column of grey form controls says "configuration utility".
 */
public class PlaneShiftTitleScreen extends Screen {

    /** One colour per action, in the order they are offered. */
    private static final int PLAY = 0xFF_E8484A;
    private static final int MAP = 0xFF_2E8AE0;
    private static final int CLOSE = 0xFF_6B6F7A;

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BUTTON_GAP = 8;

    private float time;

    public PlaneShiftTitleScreen() {
        super(Component.translatable("gui.planeshift.title"));
    }

    @Override
    protected void init() {
        int centreX = this.width / 2 - BUTTON_WIDTH / 2;
        // Anchored from the bottom rather than the middle: the wordmark needs the upper half, and
        // centring the stack put the first button through the middle of it at 16:9.
        int y = this.height - 40 - (BUTTON_HEIGHT + BUTTON_GAP) * 3;

        addRenderableWidget(new MenuButton(centreX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.planeshift.play"), PLAY, () -> {
                    ClientPacketDistributor.sendToServer(new CourseSelectPayload("course_1"));
                    this.minecraft.setScreen(null);
                }));
        y += BUTTON_HEIGHT + BUTTON_GAP;

        addRenderableWidget(new MenuButton(centreX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.planeshift.map"), MAP,
                () -> this.minecraft.setScreen(new CourseMapScreen())));
        y += BUTTON_HEIGHT + BUTTON_GAP;

        addRenderableWidget(new MenuButton(centreX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.planeshift.close"), CLOSE,
                () -> this.minecraft.setScreen(null)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        time += partialTick;
        PlaneShiftGui.renderParallaxSky(graphics, this.width, this.height, time);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centreX = this.width / 2;
        int titleY = Math.max(18, this.height / 2 - 96);

        // Wordmark: drawn several times to fake a thick outline, then a gold face over it. The
        // vanilla font has no outline mode, and a title without one disappears into a bright sky.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) {
                    graphics.drawCenteredString(this.font, this.title, centreX + dx, titleY + dy,
                            0xFF_20180C);
                }
            }
        }
        graphics.drawCenteredString(this.font, this.title, centreX, titleY, 0xFF_FFD23F);

        graphics.drawCenteredString(this.font, Component.translatable("gui.planeshift.subtitle"),
                centreX + 1, titleY + 15, 0xC0_000000);
        graphics.drawCenteredString(this.font, Component.translatable("gui.planeshift.subtitle"),
                centreX, titleY + 14, 0xFF_FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
