package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.network.LeaveCoursePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The pause menu shown instead of the vanilla one while a course is running.
 *
 * <p>The vanilla pause screen's way out is "Save and Quit to Title", which unloads the world. In a
 * course that is never what the player means — they want to stop playing *this course*, not stop
 * playing. So the exit here is Leave Course, which returns to the hub the same way finishing does,
 * clearing the clock, the auto-scroll rule and the course movement baseline on the way out.
 *
 * <p>{@link #isPauseScreen()} stays true so the integrated server still pauses in singleplayer.
 * That is the entire point of a pause menu, and the course is not unloaded by it — the world stays
 * loaded and the course state is untouched, so resuming drops the player back exactly where they
 * stood.
 */
public class CoursePauseScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 40;

    public CoursePauseScreen() {
        super(Component.translatable("gui.planeshift.pause"));
    }

    private int panelTop() {
        return Math.max(24, this.height / 3 - 20);
    }

    @Override
    protected void init() {
        int y = panelTop() + PANEL_HEIGHT + 12;

        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.pause.resume"),
                        b -> this.onClose())
                .bounds(this.width / 2 - 100, y, 200, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.pause.options"),
                        b -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)))
                .bounds(this.width / 2 - 100, y + 24, 200, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.pause.map"),
                        b -> this.minecraft.setScreen(new CourseMapScreen()))
                .bounds(this.width / 2 - 100, y + 48, 200, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.pause.leave"),
                        b -> leaveCourse())
                .bounds(this.width / 2 - 100, y + 76, 200, 20)
                .build());
    }

    private void leaveCourse() {
        ClientPacketDistributor.sendToServer(LeaveCoursePayload.INSTANCE);
        this.minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        // Straight back to the game, not back to the vanilla pause screen behind us.
        this.minecraft.setScreen(null);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlaneShiftGui.renderThemedBackground(graphics, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = panelTop();
        PlaneShiftGui.renderPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        PlaneShiftGui.drawTitle(graphics, this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, top + 14,
                PlaneShiftGui.COIN_YELLOW);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
