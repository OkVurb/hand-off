package com.studio.planeshift.client.gui;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.client.screen.CourseMapScreen;
import com.studio.planeshift.client.screen.ThreeDCourseScreen;
import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.network.CourseSelectPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Title / course select screen. Shows the PlaneShift logo and a few main actions.
 */
public class PlaneShiftTitleScreen extends Screen {

    public PlaneShiftTitleScreen() {
        super(Component.translatable("gui.planeshift.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.play"),
                b -> {
                    ClientPacketDistributor.sendToServer(new CourseSelectPayload("course_1"));
                    this.minecraft.setScreen(null);
                })
                .bounds(centerX - 100, centerY - 20, 200, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.map"),
                b -> this.minecraft.setScreen(new CourseMapScreen()))
                .bounds(centerX - 100, centerY + 10, 200, 20)
                .build());

        // 3D courses are the same generator at a wider ribbon with the camera off the rail, so
        // they belong beside the 2.5D map rather than in a separate game mode.
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.title.courses_3d"),
                b -> this.minecraft.setScreen(new ThreeDCourseScreen()))
                .bounds(centerX - 100, centerY + 40, 200, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.close"),
                b -> this.minecraft.setScreen(null))
                .bounds(centerX - 100, centerY + 70, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlaneShiftGui.renderThemedBackground(graphics, this.width, this.height);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFD700);

        graphics.drawCenteredString(this.font, Component.translatable("gui.planeshift.subtitle"),
                this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
