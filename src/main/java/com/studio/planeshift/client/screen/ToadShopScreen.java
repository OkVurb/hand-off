package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.network.ToadShopPurchasePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Toad's shop screen. Click an offer to send a purchase request to the server.
 */
public class ToadShopScreen extends Screen {

    private static final Component TITLE = Component.translatable("gui.planeshift.toad_shop.title");

    private static final int[] PRICES = {
            20, 50, 25, 50, 35, 40, 40, 40, 40, 40
    };

    public ToadShopScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;
        int buttonWidth = 180;
        int buttonHeight = 20;
        int gap = 24;

        for (int i = 0; i < PRICES.length; i++) {
            final int slot = i;
            this.addRenderableWidget(Button.builder(
                            Component.translatable("gui.planeshift.toad_shop.item." + i, PRICES[i]),
                            button -> buy(slot))
                    .bounds(centerX - buttonWidth / 2, startY + i * gap, buttonWidth, buttonHeight)
                    .build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.close"),
                        button -> onClose())
                .bounds(centerX - buttonWidth / 2, startY + PRICES.length * gap + 10, buttonWidth, buttonHeight)
                .build());
    }

    private static void buy(int slot) {
        ClientPacketDistributor.sendToServer(new ToadShopPurchasePayload(slot));
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        PlaneShiftGui.renderThemedBackground(gui, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        PlaneShiftGui.renderPanel(gui, this.width / 2 - 100, this.height / 4 - 40, 200, 34);
        PlaneShiftGui.drawTitle(gui, this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, this.height / 4 - 32, PlaneShiftGui.COIN_YELLOW);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
