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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    /** Clock colours: normal, warning band (flashing pair), and expired. */
    private static final int TIME_NORMAL = 0xFF4CEFFF;
    private static final int TIME_CRITICAL = 0xFFFF5555;
    private static final int TIME_CRITICAL_DIM = 0xFFFFAA55;
    private static final int TIME_EXPIRED = 0xFF8B2C2C;

    /** Top-left cluster panel. Height covers the score line at y+76 plus its descender. */
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 98;
    private static final int PANEL_INSET = 2;

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

        // hudScale is applied as a pose transform around the whole HUD rather than by scaling
        // every coordinate: the layout arithmetic below already handles clipping against the
        // window, and duplicating that at two scales is how panels start disagreeing with their
        // contents. guiWidth/guiHeight are read back through the scale so the clipping still
        // refers to real screen space.
        float hudScale = PlaneShiftConfig.CLIENT.hudScale.get().floatValue();
        boolean scaled = Math.abs(hudScale - 1.0F) > 0.001F;
        if (scaled) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(hudScale, hudScale);
        }
        try {
            renderCluster(graphics, font, minecraft, state, x, y, hudScale);
            renderKeybindHints(graphics, font, minecraft, hudScale);
        } finally {
            if (scaled) {
                graphics.pose().popMatrix();
            }
        }
    }

    private static void renderCluster(GuiGraphics graphics, Font font, Minecraft minecraft,
                                      CourseState state, int x, int y, float hudScale) {

        // Panel sized to the window, not fixed: at a large GUI scale the usable width can be
        // narrower than the 200px this used to assume, and the old 76px height cut off the
        // star-coin line that sits at y+64.
        int usableWidth = (int) (graphics.guiWidth() / hudScale);
        int usableHeight = (int) (graphics.guiHeight() / hudScale);
        int panelWidth = Math.min(PANEL_WIDTH, usableWidth - 2 * PANEL_INSET);
        int panelHeight = Math.min(PANEL_HEIGHT, usableHeight - 2 * PANEL_INSET);
        PlaneShiftGui.renderPanel(graphics, PANEL_INSET, PANEL_INSET, panelWidth, panelHeight);
        int panelRight = PANEL_INSET + panelWidth;
        int panelBottom = PANEL_INSET + panelHeight;

        // Health pips + Form buffer.
        for (int i = 0; i < CourseState.MAX_PIPS; i++) {
            int color = i < state.pips() ? PIP_FULL : PIP_EMPTY;
            int pipLeft = x + i * 14;
            if (pipLeft + 12 > panelRight) {
                break;   // never spill pips past the panel edge
            }
            graphics.fill(pipLeft, y, pipLeft + 12, y + 12, color);
        }
        FormSlot slot = state.formSlot();
        if (slot.hasActive()) {
            Identifier form = slot.active().get();
            int accent = 0xFFFFFFFF;
            graphics.fill(x + CourseState.MAX_PIPS * 14 + 2, y, x + CourseState.MAX_PIPS * 14 + 14, y + 12, accent & 0x50FFFFFF);
            int labelX = x + CourseState.MAX_PIPS * 14 + 18;
            graphics.drawString(font, clip(font,
                            Component.translatableWithFallback(
                                            "form." + form.getNamespace() + "." + form.getPath(), form.getPath())
                                    .append(" x" + slot.charges()),
                            panelRight - labelX),
                    labelX, y + 2, 0xFFFFFFFF);
        }
        // Clock. A timed course shows the countdown that can actually kill the player; an
        // untimed one falls back to elapsed time, which is only informational.
        if (state.timed()) {
            int secondsLeft = Math.max(0, (state.timeLeft() + 19) / 20);
            // Flash once a second inside the warning band so the danger is visible, not just read.
            boolean flash = state.timeCritical()
                    && minecraft.level != null
                    && (minecraft.level.getGameTime() / 10L) % 2L == 0L;
            int colour = state.timeExpired() ? TIME_EXPIRED
                    : state.timeCritical() ? (flash ? TIME_CRITICAL : TIME_CRITICAL_DIM)
                    : TIME_NORMAL;
            graphics.drawString(font,
                    Component.translatable("hud.planeshift.time_left", secondsLeft), x, y + 28, colour);
        } else if (minecraft.level != null && courseStartTicks >= 0L) {
            long elapsed = minecraft.level.getGameTime() - courseStartTicks;
            int totalSeconds = (int) (elapsed / 20L);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            int centi = (int) ((elapsed % 20L) * 5L);
            String time = String.format("%d:%02d.%02d", minutes, seconds, centi);
            graphics.drawString(font, Component.translatable("hud.planeshift.time", time),
                    x, y + 28, TIME_NORMAL);
        }

        // Lives, coins, star coins and score.
        graphics.drawString(font, Component.translatable("hud.planeshift.lives", state.lives()),
                x, y + 40, 0xFF4CFF4C);
        graphics.drawString(font, Component.translatable("hud.planeshift.coins", state.coins()),
                x, y + 52, 0xFFE7D07A);
        graphics.drawString(font, Component.translatable("hud.planeshift.star_coins", state.starCoins()),
                x, y + 64, 0xFFFFA500);
        graphics.drawString(font, Component.translatable("hud.planeshift.score", state.score()),
                x, y + 76, 0xFFFFFFFF);

        ScorePopups.render(graphics, font);

        // Mode badge and transition progress.
        if (PlaneShiftConfig.CLIENT.showModeBadge.get()) {
            renderModeBadge(graphics, font, state, usableWidth);
        }

        if (PlaneShiftConfig.CLIENT.showDebugHud.get()) {
            renderDebug(graphics, font, state, panelBottom + 6);
        }

        renderKeybinds(graphics, font, usableWidth, usableHeight);
    }

    private static void renderKeybinds(GuiGraphics graphics, Font font, int usableWidth, int usableHeight) {
        Minecraft mc = Minecraft.getInstance();
        String[] binds = {
            "[" + mc.options.keyJump.getTranslatedKeyMessage().getString() + "] Jump",
            "[" + mc.options.keyShift.getTranslatedKeyMessage().getString() + "] Crouch / Spin / Pound",
            "[" + com.studio.planeshift.client.PlaneShiftKeybinds.FORM_ACTION.getTranslatedKeyMessage().getString() + "] Action",
            "[" + com.studio.planeshift.client.PlaneShiftKeybinds.SWAP_RESERVE.getTranslatedKeyMessage().getString() + "] Swap Item"
        };
        int y = usableHeight - 10 - (binds.length * 12);
        for (String bind : binds) {
            int width = font.width(bind);
            graphics.drawString(font, bind, usableWidth - width - 8, y, 0xAAAAAAAA);
            y += 12;
        }
    }

    /** Trims {@code text} to {@code maxWidth} so a long form name cannot run past the panel. */
    private static String clip(Font font, Component text, int maxWidth) {
        String plain = text.getString();
        return font.width(plain) <= maxWidth ? plain : font.plainSubstrByWidth(plain, maxWidth);
    }

    private static void renderModeBadge(GuiGraphics graphics, Font font, CourseState state, int usableWidth) {
        int y = 8;
        
        if (state.formSlot().reserve().isPresent()) {
            Identifier reserve = state.formSlot().reserve().get();
            var item = BuiltInRegistries.ITEM.getValue(reserve);
            if (item != Items.AIR) {
                int boxSize = 24;
                int boxX = usableWidth / 2 - boxSize / 2;
                int boxY = y + 16;
                graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, BAR_BACK);
                graphics.fill(boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, 0x50FFFFFF);
                graphics.renderItem(new ItemStack(item), boxX + 4, boxY + 4);
            }
        }

        if (state.transition().isPresent()) {
            TransitionSync sync = state.transition().get();
            long gameTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0L;
            float progress = sync.progress(gameTime, 0.0F);
            int barWidth = 60;
            int barX = usableWidth / 2 - barWidth / 2;
            graphics.fill(barX - 1, y - 1, barX + barWidth + 1, y + 5, BAR_BACK);
            graphics.fill(barX, y, barX + (int) (barWidth * progress), y + 4, BADGE_SIDE);
            graphics.drawCenteredString(font, Component.translatable("hud.planeshift.shifting"),
                    usableWidth / 2, y + 8, 0xFFFFFFFF);
            return;
        }
        Component label = state.in2_5D()
                ? Component.translatable("hud.planeshift.mode.side_on")
                : Component.translatable("hud.planeshift.mode.free_3d");
        int color = state.in2_5D() ? BADGE_SIDE : BADGE_FREE;
        graphics.drawCenteredString(font, label, usableWidth / 2, y, color);
    }

    /**
     * Debug lines used to start at a fixed y=48, drawing straight over the lives, coins and
     * star-coin readouts at y+40..y+64. They now start below the panel.
     */
    private static void renderDebug(GuiGraphics graphics, Font font, CourseState state, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(font, "state: " + state.state().getSerializedName()
                + "  mode: " + state.mode().getSerializedName(), 8, y, 0xFFAAAAAA);
        if (state.rail().isPresent() && minecraft.player != null) {
            var rail = state.rail().get();
            double drift = rail.depthOf(minecraft.player.position()) - rail.planeCoord();
            graphics.drawString(font, String.format("rail: %s @ %.2f  drift: %+.3f",
                    rail.travelAxis(), rail.planeCoord(), drift), 8, y + 10, 0xFFAAAAAA);
        }
    }

    /**
     * Bedrock-style control hints in the bottom corners.
     *
     * <p>Static text rather than the live keybinds: {@code minecraft.options.keyJump.getTranslatedKeyMessage()}
     * would follow a rebind, and this does not. Left as-is because it is what was written, but a
     * player who has remapped jump is being told the wrong key, so this is a real TODO.
     */
    private static void renderKeybindHints(GuiGraphics graphics, Font font, Minecraft minecraft, float hudScale) {
        int usableWidth = (int) (graphics.guiWidth() / hudScale);
        int usableHeight = (int) (graphics.guiHeight() / hudScale);
        
        // Bedrock-style controller hints in bottom corners
        int bottomY = usableHeight - 15;
        
        // Bottom Left: Movement/Jump
        graphics.drawString(font, "Jump [SPACE]", 10, bottomY - 12, 0xFFFFFFFF, true);
        graphics.drawString(font, "Crouch / Warp [SHIFT]", 10, bottomY, 0xFFFFFFFF, true);
        
        // Bottom Right: Actions
        String runStr = "Action / Run [L-CLICK]";
        int runWidth = font.width(runStr);
        graphics.drawString(font, runStr, usableWidth - runWidth - 10, bottomY - 12, 0xFFFFFFFF, true);
        
        String useStr = "Use [R-CLICK]";
        int useWidth = font.width(useStr);
        graphics.drawString(font, useStr, usableWidth - useWidth - 10, bottomY, 0xFFFFFFFF, true);
    }
}
