package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.course.CourseProgress;
import com.studio.planeshift.common.network.CourseResultsPayload;
import net.minecraft.client.Minecraft;
import com.studio.planeshift.common.registry.ModAttachments;
import com.studio.planeshift.common.course.WorldRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The course-clear breakdown.
 *
 * <p>Everything shown comes from the payload rather than from the synced course state, because
 * clearing a course resets that state: by the time this opens, the score and the clock the player
 * just earned no longer exist anywhere else.
 */
public class CourseResultsScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int ROW_HEIGHT = 14;

    private final CourseResultsPayload results;

    public CourseResultsScreen(CourseResultsPayload results) {
        super(Component.translatable("gui.planeshift.results"));
        this.results = results;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.results.continue"),
                        b -> this.minecraft.setScreen(new CourseMapScreen()))
                .bounds(this.width / 2 - 100, panelTop() + panelHeight() + 10, 200, 20)
                .build());
    }

    private int panelHeight() {
        return 40 + ROW_HEIGHT * 6;
    }

    private int panelTop() {
        return Math.max(20, this.height / 2 - panelHeight() / 2 - 20);
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
        PlaneShiftGui.renderPanel(graphics, left, top, PANEL_WIDTH, panelHeight());

        PlaneShiftGui.drawTitle(graphics, this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, top + 8, PlaneShiftGui.COIN_YELLOW);

        int y = top + 28;
        row(graphics, left, y, "gui.planeshift.results.course",
                Component.translatable("course.planeshift." + results.courseId()));
        y += ROW_HEIGHT;
        row(graphics, left, y, "gui.planeshift.results.time_left",
                Component.literal(formatClock(results.timeLeft())));
        y += ROW_HEIGHT;
        row(graphics, left, y, "gui.planeshift.results.time_bonus",
                Component.literal(Integer.toString(results.timeBonus())));
        y += ROW_HEIGHT;
        row(graphics, left, y, "gui.planeshift.results.coins",
                Component.literal(Integer.toString(results.coins())));
        y += ROW_HEIGHT;
        row(graphics, left, y, "gui.planeshift.results.star_coins",
                Component.literal(results.starCoins() + " / " + CourseProgress.STAR_COINS_PER_COURSE));
        y += ROW_HEIGHT;
        row(graphics, left, y, "gui.planeshift.results.score",
                Component.literal(Integer.toString(results.score())));

        drawUnlockBanner(graphics, top, panelHeight());

        if (results.newBestScore()) {
            Component best = Component.translatable("gui.planeshift.results.new_best");
            graphics.drawString(this.font, best,
                    this.width / 2 - this.font.width(best) / 2, top + panelHeight() - 12,
                    PlaneShiftGui.COIN_YELLOW, true);
        }
    }

    /**
     * Announces the moment the final world opens.
     *
     * <p>A lock quietly becoming unlocked is invisible. The player who finally crosses the
     * star-coin threshold is looking at this screen, not at the map, and without saying so the
     * only evidence is a node that stopped being grey on a screen they may not open for a while --
     * so the collectable that gates it never gets credited with having done anything.
     *
     * <p>Drawn below the panel rather than inside it, because the rows above are about this course
     * and this is about the game. Putting it in the list would make the largest thing that has
     * happened all run look like another statistic.
     */
    private void drawUnlockBanner(GuiGraphics graphics, int top, int panelHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        CourseProgress progress = minecraft.player.getData(ModAttachments.COURSE_PROGRESS);
        if (!WorldRegistry.justOpenedFinalWorld(progress, results.courseId(),
                results.starCoins())) {
            return;
        }
        Component banner = Component.translatable("gui.planeshift.banner.final_world");
        int y = top + panelHeight + 10;
        int w = this.font.width(banner);
        int x = this.width / 2 - w / 2;
        graphics.fill(x - 8, y - 4, x + w + 8, y + 12, 0xCC_000000);
        graphics.fill(x - 8, y - 4, x + w + 8, y - 3, PlaneShiftGui.COIN_YELLOW);
        graphics.fill(x - 8, y + 11, x + w + 8, y + 12, PlaneShiftGui.COIN_YELLOW);
        graphics.drawString(this.font, banner, x, y, PlaneShiftGui.COIN_YELLOW, true);
    }

    private void row(GuiGraphics graphics, int left, int y, String labelKey, Component value) {
        graphics.drawString(this.font, Component.translatable(labelKey), left + 12, y, 0xFFFFFFFF, true);
        graphics.drawString(this.font, value,
                left + PANEL_WIDTH - 12 - this.font.width(value), y, PlaneShiftGui.COIN_YELLOW, true);
    }

    /** Ticks to m:ss. An untimed course reports zero, which reads as a dash rather than 0:00. */
    private static String formatClock(int ticks) {
        if (ticks <= 0) {
            return "--";
        }
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
