package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.network.CourseSelectPayload;
import com.studio.planeshift.common.network.GameOverPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Shown when the last life is spent.
 *
 * <p>Only a game over gets a screen. Losing a single life returns the player to their checkpoint
 * immediately and always has — interrupting that with a dialog would break the one thing a
 * platformer has to protect, which is the loop of dying and instantly trying again. A run ending
 * is different: there is nothing to retry into without a decision, so the decision is offered.
 *
 * <p>Retry re-enters the course that was just lost rather than dropping the player on the map to
 * find it again.
 */
public class CourseGameOverScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 96;
    private static final int GAME_OVER_RED = 0xFF_E7514E;

    private final GameOverPayload results;

    public CourseGameOverScreen(GameOverPayload results) {
        super(Component.translatable("gui.planeshift.game_over"));
        this.results = results;
    }

    private int panelTop() {
        return Math.max(24, this.height / 2 - PANEL_HEIGHT / 2 - 20);
    }

    @Override
    protected void init() {
        int y = panelTop() + PANEL_HEIGHT + 10;

        // Only offered when the server told us which course was lost. A retry button that cannot
        // say what it would retry is worse than no button.
        if (!results.courseId().isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.game_over.retry"),
                            b -> retry())
                    .bounds(this.width / 2 - 100, y, 200, 20)
                    .build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.game_over.map"),
                        b -> this.minecraft.setScreen(new CourseMapScreen()))
                .bounds(this.width / 2 - 100, y, 200, 20)
                .build());
    }

    private void retry() {
        ClientPacketDistributor.sendToServer(new CourseSelectPayload(results.courseId()));
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
                this.width / 2 - this.font.width(this.title) / 2, top + 14, GAME_OVER_RED);

        Component course = Component.translatable("course.planeshift." + results.courseId());
        Component score = Component.translatable("gui.planeshift.game_over.score", results.score());
        graphics.drawString(this.font, course,
                this.width / 2 - this.font.width(course) / 2, top + 44, 0xFF_FFFFFF, true);
        graphics.drawString(this.font, score,
                this.width / 2 - this.font.width(score) / 2, top + 62, PlaneShiftGui.COIN_YELLOW, true);
    }

    /**
     * Escape must not silently dismiss this. The player has a decision to make, and closing the
     * screen with no choice leaves them standing in the hub with no idea what happened.
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
