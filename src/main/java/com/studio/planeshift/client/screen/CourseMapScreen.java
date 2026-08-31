package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.network.CourseSelectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Top-down course-select map (Design Bible, "World map").
 *
 * <p>Right now this is a vertical-slice placeholder: a parchment-style menu with
 * buttons for each course. Later it will render a real map, a walking player icon,
 * and unlock paths. Clicking a course will load the course dimension and teleport
 * the player.
 */
public class CourseMapScreen extends Screen {

    private static final Component TITLE = Component.translatable("gui.planeshift.course_map");
    private static final int COURSE_COUNT = 5;

    public CourseMapScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int y = Math.max(52, this.height / 4);
        for (int course = 1; course <= COURSE_COUNT; course++) {
            int courseNumber = course;
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.planeshift.course_map.course_" + courseNumber),
                            b -> selectCourse("course_" + courseNumber))
                    .pos(this.width / 2 - 100, y + (courseNumber - 1) * 26)
                    .size(200, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.course_map.close"), b -> this.onClose())
                .pos(this.width / 2 - 100, y + COURSE_COUNT * 26 + 8)
                .size(200, 20)
                .build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlaneShiftGui.renderThemedBackground(graphics, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int titleY = Math.max(30, this.height / 6 - 20);
        PlaneShiftGui.renderPanel(graphics, this.width / 2 - 110, titleY - 10, 220, 30);
        PlaneShiftGui.drawTitle(graphics, this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, titleY,
                PlaneShiftGui.COIN_YELLOW);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void selectCourse(String courseId) {
        ClientPacketDistributor.sendToServer(new CourseSelectPayload(courseId));
        Minecraft.getInstance().setScreen(null);
    }
}
