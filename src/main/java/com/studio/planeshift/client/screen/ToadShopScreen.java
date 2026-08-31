package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.network.ToadShopPurchasePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Toad's shop screen. Click an offer to send a purchase request to the server.
 *
 * <p>The layout is computed from the window rather than fixed. The previous version started at
 * {@code height / 4} and stepped 24px per offer, needing 270px below the quarter mark for ten
 * offers plus the close button — more than exists at a large GUI scale, where the last offers
 * and the close button fell off the bottom with no way to reach them. Here the grid picks a
 * column count and row spacing that fit the space actually available, so every offer stays
 * clickable at any window size or GUI scale.
 */
public class ToadShopScreen extends Screen {

    private static final Component TITLE = Component.translatable("gui.planeshift.toad_shop.title");

    private static final int[] PRICES = {
            20, 50, 25, 50, 35, 40, 40, 40, 40, 40
    };

    private static final int BUTTON_HEIGHT = 20;
    private static final int PREFERRED_BUTTON_WIDTH = 180;
    private static final int MIN_BUTTON_WIDTH = 80;
    private static final int MARGIN = 6;
    private static final int COLUMN_GAP = 6;
    /** Title panel height plus the gap beneath it. */
    private static final int HEADER_HEIGHT = 44;
    /** Tightest row spacing that still leaves buttons visually separated. */
    private static final int MIN_ROW_STEP = BUTTON_HEIGHT + 2;
    private static final int PREFERRED_ROW_STEP = 24;
    private static final int MAX_COLUMNS = 3;

    private int panelX;
    private int panelY;
    private int panelWidth;

    public ToadShopScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int offers = PRICES.length;
        // The header is pinned to the top rather than derived from height/4, which could push it
        // off the top of a short window.
        int gridTop = MARGIN + HEADER_HEIGHT;
        int available = Math.max(MIN_ROW_STEP, this.height - gridTop - MARGIN);

        // Fewest columns whose rows — offers plus the close button's own row — fit the space.
        int columns = 1;
        while (columns < MAX_COLUMNS
                && (rowsFor(offers, columns) + 1) * MIN_ROW_STEP > available) {
            columns++;
        }

        int rows = rowsFor(offers, columns);
        int rowStep = Mth.clamp(available / (rows + 1), MIN_ROW_STEP, PREFERRED_ROW_STEP);

        int widthForColumns = this.width - 2 * MARGIN - (columns - 1) * COLUMN_GAP;
        int buttonWidth = Mth.clamp(widthForColumns / columns, MIN_BUTTON_WIDTH, PREFERRED_BUTTON_WIDTH);

        int gridWidth = columns * buttonWidth + (columns - 1) * COLUMN_GAP;
        int left = (this.width - gridWidth) / 2;

        for (int i = 0; i < offers; i++) {
            final int slot = i;
            int column = i % columns;
            int row = i / columns;
            this.addRenderableWidget(Button.builder(
                            Component.translatable("gui.planeshift.toad_shop.item." + i, PRICES[i]),
                            button -> buy(slot))
                    .bounds(left + column * (buttonWidth + COLUMN_GAP),
                            gridTop + row * rowStep,
                            buttonWidth, BUTTON_HEIGHT)
                    .build());
        }

        // Close sits on its own row under the grid, clamped so it can never leave the window.
        int closeWidth = Math.min(PREFERRED_BUTTON_WIDTH, this.width - 2 * MARGIN);
        int closeY = Math.min(gridTop + rows * rowStep, this.height - MARGIN - BUTTON_HEIGHT);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.close"),
                        button -> onClose())
                .bounds((this.width - closeWidth) / 2, closeY, closeWidth, BUTTON_HEIGHT)
                .build());

        this.panelWidth = Math.min(200, this.width - 2 * MARGIN);
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = MARGIN;
    }

    private static int rowsFor(int count, int columns) {
        return (count + columns - 1) / columns;
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
        PlaneShiftGui.renderPanel(gui, panelX, panelY, panelWidth, 34);
        PlaneShiftGui.drawTitle(gui, this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, panelY + 8, PlaneShiftGui.COIN_YELLOW);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
