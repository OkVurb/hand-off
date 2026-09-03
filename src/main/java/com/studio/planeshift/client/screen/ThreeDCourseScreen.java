package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.network.CourseSelectPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Course select for the 3D courses.
 *
 * <p>Separate from {@link CourseMapScreen} because the two are different promises, not different
 * content. The world map is a progression: ten numbered nodes, locked until earned. These are a
 * short list of open spaces to move around in, with no unlock chain, so presenting them on the
 * same map would imply an ordering that does not exist.
 *
 * <p>The courses themselves come from the same generator. A 3D course is the identical segment
 * sequence built at {@code WIDE_HALF_WIDTH} with the camera off the rail — which is the whole
 * argument for keeping 2.5D and 3D in one mod rather than two.
 */
public class ThreeDCourseScreen extends Screen {

    private static final List<String> COURSES = List.of("space_1", "space_2", "space_3");

    public ThreeDCourseScreen() {
        super(Component.translatable("gui.planeshift.title.courses_3d"));
    }

    @Override
    protected void init() {
        int y = Math.max(60, this.height / 3);
        for (int i = 0; i < COURSES.size(); i++) {
            String id = COURSES.get(i);
            addRenderableWidget(Button.builder(
                            Component.translatable("course.planeshift." + id),
                            b -> enter(id))
                    .bounds(this.width / 2 - 100, y + i * 26, 200, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.close"),
                        b -> this.onClose())
                .bounds(this.width / 2 - 100, y + COURSES.size() * 26 + 12, 200, 20)
                .build());
    }

    private void enter(String courseId) {
        ClientPacketDistributor.sendToServer(new CourseSelectPayload(courseId));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlaneShiftGui.renderThemedBackground(graphics, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        PlaneShiftGui.drawTitle(graphics, this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, Math.max(24, this.height / 6),
                PlaneShiftGui.COIN_YELLOW);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
