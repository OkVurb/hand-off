package com.studio.planeshift.client.hud;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.PlaneShiftConfig;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.form.FormSlot;
import com.studio.planeshift.common.mode.TransitionSync;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Course HUD (Design Bible, "HUD, menus, and feedback").
 *
 * <p>"The HUD answers four questions instantly: what state am I in, what can I do,
 * where is safety, and what did the game just confirm?" — state badge, form/charges,
 * pips, lives, and coin counter. Drawn with flat primitives so it needs no textures and
 * remains readable under every resource pack.
 */
public final class CourseHud {

    private static final int PIP_FULL = 0xFFE7514E;
    private static final int PIP_EMPTY = 0xFF3A2A2A;
    private static final int BADGE_SIDE = 0xFF3ECFCB;   // teal seam language
    private static final int BADGE_FREE = 0xFFE7B54A;   // gold
    private static final int BAR_BACK = 0xB0101418;

    private static long courseStartTicks = -1L;

    private CourseHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        CourseState state = ClientCourseState.get();
        if (state.isHub() && state.transition().isEmpty()) {
            courseStartTicks = -1L;
            return;
        }
        if (minecraft.level != null && courseStartTicks < 0L) {
            courseStartTicks = minecraft.level.getGameTime();
        }
        Font font = minecraft.font;
        int x = 8;
        int y = 8;

        // Draw a themed backing panel behind the top-left HUD cluster.
        PlaneShiftGui.renderPanel(graphics, 2, 2, 200, 76);

        // Health pips + Form buffer.
        for (int i = 0; i < CourseState.MAX_PIPS; i++) {
            int color = i < state.pips() ? PIP_FULL : PIP_EMPTY;
            graphics.fill(x + i * 14, y, x + i * 14 + 12, y + 12, color);
        }
        FormSlot slot = state.formSlot();
        if (slot.hasActive()) {
            Identifier form = slot.active().get();
            int accent = 0xFFFFFFFF;
            graphics.fill(x + CourseState.MAX_PIPS * 14 + 2, y, x + CourseState.MAX_PIPS * 14 + 14, y + 12, accent & 0x50FFFFFF);
            graphics.drawString(font,
                    Component.translatableWithFallback(
                                    "form." + form.getNamespace() + "." + form.getPath(), form.getPath())
                            .append(" x" + slot.charges()),
                    x + CourseState.MAX_PIPS * 14 + 18, y + 2, 0xFFFFFFFF);
        }
        if (slot.reserve().isPresent()) {
            Identifier reserve = slot.reserve().get();
            graphics.drawString(font,
                    Component.translatable("hud.planeshift.reserve",
                            Component.translatableWithFallback(
                                    "form." + reserve.getNamespace() + "." + reserve.getPath(),
                                    reserve.getPath())),
                    x, y + 16, 0xFF9DA8B5);
        }

        // Timer.
        if (minecraft.level != null && courseStartTicks >= 0L) {
            long elapsed = minecraft.level.getGameTime() - courseStartTicks;
            int totalSeconds = (int) (elapsed / 20L);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            int centi = (int) ((elapsed % 20L) * 5L);
            String time = String.format("%d:%02d.%02d", minutes, seconds, centi);
            graphics.drawString(font, Component.translatable("hud.planeshift.time", time),
                    x, y + 28, 0xFF4CEFFF);
        }

        // Lives, coins and star coins.
        graphics.drawString(font, Component.translatable("hud.planeshift.lives", state.lives()),
                x, y + 40, 0xFF4CFF4C);
        graphics.drawString(font, Component.translatable("hud.planeshift.coins", state.coins()),
                x, y + 52, 0xFFE7D07A);
        graphics.drawString(font, Component.translatable("hud.planeshift.star_coins", state.starCoins()),
                x, y + 64, 0xFFFFA500);

        // Mode badge and transition progress.
        if (PlaneShiftConfig.CLIENT.showModeBadge.get()) {
            renderModeBadge(graphics, font, state);
        }

        if (PlaneShiftConfig.CLIENT.showDebugHud.get()) {
            renderDebug(graphics, font, state);
        }
    }

    private static void renderModeBadge(GuiGraphics graphics, Font font, CourseState state) {
        int screenWidth = graphics.guiWidth();
        int y = 8;
        if (state.transition().isPresent()) {
            TransitionSync sync = state.transition().get();
            long gameTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0L;
            float progress = sync.progress(gameTime, 0.0F);
            int barWidth = 60;
            int barX = screenWidth / 2 - barWidth / 2;
            graphics.fill(barX - 1, y - 1, barX + barWidth + 1, y + 5, BAR_BACK);
            graphics.fill(barX, y, barX + (int) (barWidth * progress), y + 4, BADGE_SIDE);
            graphics.drawCenteredString(font, Component.translatable("hud.planeshift.shifting"),
                    screenWidth / 2, y + 8, 0xFFFFFFFF);
            return;
        }
        Component label = state.in2_5D()
                ? Component.translatable("hud.planeshift.mode.side_on")
                : Component.translatable("hud.planeshift.mode.free_3d");
        int color = state.in2_5D() ? BADGE_SIDE : BADGE_FREE;
        graphics.drawCenteredString(font, label, screenWidth / 2, y, color);
    }

    private static void renderDebug(GuiGraphics graphics, Font font, CourseState state) {
        Minecraft minecraft = Minecraft.getInstance();
        int y = 48;
        graphics.drawString(font, "state: " + state.state().getSerializedName()
                + "  mode: " + state.mode().getSerializedName(), 8, y, 0xFFAAAAAA);
        if (state.rail().isPresent() && minecraft.player != null) {
            var rail = state.rail().get();
            double drift = rail.depthOf(minecraft.player.position()) - rail.planeCoord();
            graphics.drawString(font, String.format("rail: %s @ %.2f  drift: %+.3f",
                    rail.travelAxis(), rail.planeCoord(), drift), 8, y + 10, 0xFFAAAAAA);
        }
        state.transition().ifPresent(sync -> graphics.drawString(font,
                "tx: " + sync.txId() + " " + sync.fromMode() + " -> " + sync.toMode(),
                8, y + 20, 0xFFAAAAAA));
    }
}
