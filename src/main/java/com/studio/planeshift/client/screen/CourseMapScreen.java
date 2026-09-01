package com.studio.planeshift.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.course.CourseProgress;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.network.CourseSelectPayload;
import com.studio.planeshift.common.registry.ModAttachments;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The world map: one world at a time, its ten courses laid out as a path of nodes.
 *
 * <p>Locked courses are drawn locked and refuse the click. That is a convenience, not a
 * protection — the server refuses a locked course independently, so a client that skips this
 * screen entirely gains nothing. The screen exists to tell the truth about what is available,
 * and it reads that truth from the same {@link WorldRegistry} rule the server enforces.
 */
public class CourseMapScreen extends Screen {

    private static final int NODE_SIZE = 22;
    private static final int NODE_GAP = 12;
    private static final int PATH_THICKNESS = 3;

    private static final int LOCKED = 0xFF_44484F;
    private static final int LOCKED_EDGE = 0xFF_2A2D33;
    private static final int OPEN = 0xFF_2E9BDC;
    private static final int OPEN_EDGE = 0xFF_1A5F8A;
    private static final int CLEARED = 0xFF_3FBF5F;
    private static final int CLEARED_EDGE = 0xFF_1F7A38;
    private static final int BOSS = 0xFF_D8503C;
    private static final int BOSS_EDGE = 0xFF_8A2A1C;
    private static final int PATH = 0xFF_C9B182;
    private static final int SELECTED_RING = 0xFF_FFD700;

    private int worldIndex;
    private int selected;
    private Button previousWorld;
    private Button nextWorld;

    public CourseMapScreen() {
        this(0);
    }

    public CourseMapScreen(int worldIndex) {
        super(Component.translatable("gui.planeshift.course_map"));
        this.worldIndex = Mth.clamp(worldIndex, 0, WorldRegistry.worldCount() - 1);
    }

    /**
     * The player's saved progress, read from the synced attachment.
     *
     * <p>Falls back to an empty record when there is no player — the screen can legitimately be
     * constructed a frame before the client player exists, and an empty record simply draws
     * everything as unplayed rather than crashing.
     */
    private static CourseProgress progress() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null
                ? CourseProgress.DEFAULT
                : minecraft.player.getData(ModAttachments.COURSE_PROGRESS);
    }

    private WorldDefinition world() {
        return WorldRegistry.allWorlds().get(worldIndex);
    }

    @Override
    protected void init() {
        int buttonY = rowY() + NODE_SIZE + 44;

        previousWorld = addRenderableWidget(Button.builder(
                        Component.literal("<"), b -> changeWorld(-1))
                .bounds(this.width / 2 - 150, buttonY, 20, 20)
                .build());
        nextWorld = addRenderableWidget(Button.builder(
                        Component.literal(">"), b -> changeWorld(1))
                .bounds(this.width / 2 + 130, buttonY, 20, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.course_map.play"),
                        b -> enterSelected())
                .bounds(this.width / 2 - 100, buttonY, 200, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.course_map.close"),
                        b -> this.onClose())
                .bounds(this.width / 2 - 100, buttonY + 24, 200, 20)
                .build());

        updateArrows();
    }

    private void changeWorld(int delta) {
        worldIndex = Mth.clamp(worldIndex + delta, 0, WorldRegistry.worldCount() - 1);
        selected = 0;
        updateArrows();
    }

    private void updateArrows() {
        previousWorld.active = worldIndex > 0;
        // The next world only becomes reachable once this one has been opened at all; showing it
        // as clickable when its first course is locked would promise something the server refuses.
        nextWorld.active = worldIndex < WorldRegistry.worldCount() - 1
                && WorldRegistry.isWorldUnlocked(progress(),
                        WorldRegistry.allWorlds().get(worldIndex + 1));
    }

    private int rowWidth() {
        return WorldDefinition.COURSES_PER_WORLD * NODE_SIZE
                + (WorldDefinition.COURSES_PER_WORLD - 1) * NODE_GAP;
    }

    private int rowLeft() {
        return this.width / 2 - rowWidth() / 2;
    }

    private int rowY() {
        return Math.max(70, this.height / 2 - NODE_SIZE);
    }

    private int nodeX(int index) {
        return rowLeft() + index * (NODE_SIZE + NODE_GAP);
    }

    private void enterSelected() {
        List<String> courses = world().courseIds();
        String courseId = courses.get(selected);
        if (!WorldRegistry.isUnlocked(progress(), courseId)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new CourseSelectPayload(courseId));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int y = rowY();
        for (int i = 0; i < WorldDefinition.COURSES_PER_WORLD; i++) {
            int x = nodeX(i);
            if (event.x() >= x && event.x() < x + NODE_SIZE
                    && event.y() >= y && event.y() < y + NODE_SIZE) {
                // First click selects, second enters. Selecting first means a misclick on a
                // neighbouring node costs nothing, which matters on a dense row of ten.
                if (selected == i) {
                    enterSelected();
                } else {
                    selected = i;
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Arrow keys walk the path, which is how a map like this is meant to be driven.
        switch (event.key()) {
            case InputConstants.KEY_LEFT -> {
                selected = Math.max(0, selected - 1);
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                selected = Math.min(WorldDefinition.COURSES_PER_WORLD - 1, selected + 1);
                return true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                enterSelected();
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlaneShiftGui.renderThemedBackground(graphics, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        CourseProgress progress = progress();
        WorldDefinition world = world();
        List<String> courses = world.courseIds();

        int titleY = Math.max(24, rowY() - 46);
        PlaneShiftGui.renderPanel(graphics, this.width / 2 - 130, titleY - 8, 260, 30);
        Component heading = Component.translatable("gui.planeshift.course_map.world",
                worldIndex + 1, Component.translatableWithFallback(world.nameKey(), world.displayName()));
        PlaneShiftGui.drawTitle(graphics, this.font, heading,
                this.width / 2 - this.font.width(heading) / 2, titleY, PlaneShiftGui.COIN_YELLOW);

        int y = rowY();

        // The path first, so the nodes sit on top of it.
        int pathY = y + NODE_SIZE / 2 - PATH_THICKNESS / 2;
        graphics.fill(nodeX(0) + NODE_SIZE / 2, pathY,
                nodeX(WorldDefinition.COURSES_PER_WORLD - 1) + NODE_SIZE / 2,
                pathY + PATH_THICKNESS, PATH);

        for (int i = 0; i < WorldDefinition.COURSES_PER_WORLD; i++) {
            String courseId = courses.get(i);
            boolean unlocked = WorldRegistry.isUnlocked(progress, courseId);
            boolean cleared = progress.cleared(courseId);
            boolean boss = i == WorldDefinition.COURSES_PER_WORLD - 1;

            int fill;
            int edge;
            if (!unlocked) {
                fill = LOCKED;
                edge = LOCKED_EDGE;
            } else if (cleared) {
                fill = CLEARED;
                edge = CLEARED_EDGE;
            } else if (boss) {
                fill = BOSS;
                edge = BOSS_EDGE;
            } else {
                fill = OPEN;
                edge = OPEN_EDGE;
            }

            int x = nodeX(i);
            if (i == selected) {
                graphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, SELECTED_RING);
            }
            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, edge);
            graphics.fill(x + 2, y + 2, x + NODE_SIZE - 2, y + NODE_SIZE - 2, fill);

            // Locked nodes show nothing rather than a number: the number is information the
            // player has not earned, and a greyed-out number reads as a bug.
            String label = unlocked ? Integer.toString(i + 1) : "✖";
            graphics.drawString(this.font, label,
                    x + NODE_SIZE / 2 - this.font.width(label) / 2, y + NODE_SIZE / 2 - 4,
                    0xFF_FFFFFF, true);

            if (unlocked) {
                drawStarCoins(graphics, x, y + NODE_SIZE + 3, progress.starCoins(courseId));
            }
        }

        drawSelectionDetail(graphics, progress, courses.get(selected), y + NODE_SIZE + 20);
    }

    /** Three pips under each node: filled for a star coin found, hollow for one still hidden. */
    private void drawStarCoins(GuiGraphics graphics, int x, int y, int found) {
        int pip = 4;
        int spacing = 6;
        int startX = x + NODE_SIZE / 2 - (CourseProgress.STAR_COINS_PER_COURSE * spacing) / 2 + 1;
        for (int i = 0; i < CourseProgress.STAR_COINS_PER_COURSE; i++) {
            int px = startX + i * spacing;
            graphics.fill(px, y, px + pip, y + pip,
                    i < found ? PlaneShiftGui.COIN_YELLOW : 0x60_000000);
        }
    }

    private void drawSelectionDetail(GuiGraphics graphics, CourseProgress progress,
                                     String courseId, int y) {
        boolean unlocked = WorldRegistry.isUnlocked(progress, courseId);
        Component line = unlocked
                ? Component.translatable("gui.planeshift.course_map.detail",
                        selected + 1, progress.record(courseId).bestScore())
                : Component.translatable("gui.planeshift.course_map.locked");
        graphics.drawString(this.font, line,
                this.width / 2 - this.font.width(line) / 2, y, 0xFF_FFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
