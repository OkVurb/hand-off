package com.studio.planeshift.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.course.CourseProgress;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldMapLayout;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.network.CourseSelectPayload;
import com.studio.planeshift.common.network.MapNodePayload;
import com.studio.planeshift.common.registry.ModAttachments;
import java.util.ArrayList;
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
 * The world map.
 *
 * <p>A winding path of nodes rather than a list of buttons, because a level select should let the
 * player see the shape of a world before they play it: how far the castle is, that there is a Toad
 * House they have not visited, that the cannon is still locked. A row of ten identical entries
 * communicates none of that.
 *
 * <p>Everything drawn here is checked against {@link WorldRegistry}'s unlock rule, the same rule
 * the server enforces on a {@code CourseSelectPayload}. The map greys out what it must; it is not
 * what makes the rule true.
 */
public class CourseMapScreen extends Screen {

    private static final int NODE = 18;
    private static final int PATH_WIDTH = 5;

    private static final int PATH = 0xFF_E8CC6A;
    private static final int PATH_EDGE = 0xFF_9A7C24;
    private static final int PATH_LOCKED = 0xFF_5C5A54;

    private static final int OPEN = 0xFF_3FA9E8;
    private static final int OPEN_EDGE = 0xFF_1B5F8C;
    private static final int CLEARED = 0xFF_49C46A;
    private static final int CLEARED_EDGE = 0xFF_1F7A38;
    private static final int LOCKED = 0xFF_50545C;
    private static final int LOCKED_EDGE = 0xFF_2A2D33;
    private static final int CASTLE = 0xFF_9C4A3A;
    private static final int CASTLE_EDGE = 0xFF_5A2418;
    private static final int TOAD = 0xFF_F2F2F2;
    private static final int TOAD_SPOT = 0xFF_E24C4C;
    private static final int CANNON = 0xFF_3C4450;
    private static final int TOKEN = 0xFF_E8342E;

    private static final int GRASS_TOP = 0xFF_66C24E;
    private static final int GRASS_BOTTOM = 0xFF_3E8F38;

    private int worldIndex;
    private int selected;
    private WorldMapLayout layout;
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
     * Whether the local player is exempt from progression gates.
     *
     * <p>Mirrors ProgressionService.bypassesLocks. The map must ask the same question the server
     * will, or it greys out courses that would in fact load.
     */
    private static boolean bypassesLocks() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    private static CourseProgress progress() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null
                ? CourseProgress.DEFAULT
                : minecraft.player.getData(ModAttachments.COURSE_PROGRESS);
    }

    private static CourseState state() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null
                ? CourseState.DEFAULT
                : minecraft.player.getData(ModAttachments.COURSE_STATE);
    }

    private WorldDefinition world() {
        return WorldRegistry.allWorlds().get(worldIndex);
    }

    @Override
    protected void init() {
        layout = WorldMapLayout.forWorld(world());
        if (selected <= 0 || selected >= layout.nodes().size()) {
            selected = firstPlayable();
        }

        int buttonY = this.height - 30;
        previousWorld = addRenderableWidget(Button.builder(Component.literal("<"),
                        b -> changeWorld(-1))
                .bounds(this.width / 2 - 158, buttonY, 20, 20).build());
        nextWorld = addRenderableWidget(Button.builder(Component.literal(">"),
                        b -> changeWorld(1))
                .bounds(this.width / 2 + 138, buttonY, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.course_map.play"),
                        b -> enterSelected())
                .bounds(this.width / 2 - 132, buttonY, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.course_map.close"),
                        b -> this.onClose())
                .bounds(this.width / 2 + 4, buttonY, 130, 20).build());

        updateArrows();
    }

    private int firstPlayable() {
        for (int i = 0; i < layout.nodes().size(); i++) {
            if (layout.node(i).isPlayable()) {
                return i;
            }
        }
        return 0;
    }

    private void changeWorld(int delta) {
        worldIndex = Mth.clamp(worldIndex + delta, 0, WorldRegistry.worldCount() - 1);
        selected = -1;
        rebuildWidgets();
    }

    private void updateArrows() {
        previousWorld.active = worldIndex > 0;
        nextWorld.active = worldIndex < WorldRegistry.worldCount() - 1
                && WorldRegistry.isWorldUnlocked(progress(),
                        WorldRegistry.allWorlds().get(worldIndex + 1), bypassesLocks());
    }

    // ------------------------------------------------------------------ geometry

    private int mapLeft() {
        return 24;
    }

    private int mapTop() {
        return 44;
    }

    private int mapWidth() {
        return this.width - 48;
    }

    private int mapHeight() {
        return this.height - 92;
    }

    private int nodeX(WorldMapLayout.Node node) {
        return mapLeft() + (int) (node.x() * mapWidth());
    }

    private int nodeY(WorldMapLayout.Node node) {
        return mapTop() + (int) (node.y() * mapHeight());
    }

    private boolean unlocked(WorldMapLayout.Node node) {
        if (!node.isPlayable()) {
            // A Toad House or cannon opens once the course it hangs off is reachable.
            return anyNeighbourUnlocked(node);
        }
        return WorldRegistry.isUnlocked(progress(), node.id(), bypassesLocks());
    }

    private boolean anyNeighbourUnlocked(WorldMapLayout.Node node) {
        int index = layout.nodes().indexOf(node);
        for (WorldMapLayout.Link link : layout.links()) {
            int other = link.from() == index ? link.to() : (link.to() == index ? link.from() : -1);
            if (other >= 0) {
                WorldMapLayout.Node neighbour = layout.node(other);
                if (neighbour.isPlayable()
                        && WorldRegistry.isUnlocked(progress(), neighbour.id(), bypassesLocks())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ input

    private void enterSelected() {
        WorldMapLayout.Node node = layout.node(selected);
        if (!unlocked(node)) {
            return;
        }
        if (node.isPlayable()) {
            ClientPacketDistributor.sendToServer(new CourseSelectPayload(node.id()));
            Minecraft.getInstance().setScreen(null);
            return;
        }
        // Toad House and cannon are map interactions, not courses; the server decides what they do.
        ClientPacketDistributor.sendToServer(new MapNodePayload(node.id()));
        Minecraft.getInstance().setScreen(null);
    }

    /** Moves the token to the nearest node in a direction, following the drawn links. */
    private void step(int dx, int dy) {
        WorldMapLayout.Node from = layout.node(selected);
        int bestIndex = -1;
        double bestScore = Double.MAX_VALUE;

        for (WorldMapLayout.Link link : layout.links()) {
            int other = link.from() == selected ? link.to()
                    : (link.to() == selected ? link.from() : -1);
            if (other < 0) {
                continue;
            }
            WorldMapLayout.Node candidate = layout.node(other);
            if (candidate.type() == WorldMapLayout.NodeType.START) {
                continue;
            }
            double ndx = candidate.x() - from.x();
            double ndy = candidate.y() - from.y();
            // Only accept a neighbour that actually lies the way the player pressed.
            if (dx != 0 && Math.signum(ndx) != Math.signum(dx)) {
                continue;
            }
            if (dy != 0 && Math.signum(ndy) != Math.signum(dy)) {
                continue;
            }
            double score = Math.abs(ndx) + Math.abs(ndy);
            if (score < bestScore) {
                bestScore = score;
                bestIndex = other;
            }
        }
        if (bestIndex >= 0) {
            selected = bestIndex;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < layout.nodes().size(); i++) {
            WorldMapLayout.Node node = layout.node(i);
            if (node.type() == WorldMapLayout.NodeType.START) {
                continue;
            }
            int x = nodeX(node);
            int y = nodeY(node);
            if (Math.abs(event.x() - x) <= NODE / 2.0 && Math.abs(event.y() - y) <= NODE / 2.0) {
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
        switch (event.key()) {
            case InputConstants.KEY_LEFT -> {
                step(-1, 0);
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                step(1, 0);
                return true;
            }
            case InputConstants.KEY_UP -> {
                step(0, -1);
                return true;
            }
            case InputConstants.KEY_DOWN -> {
                step(0, 1);
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

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // A grassy field rather than the shared themed panel: this screen is a place, and the
        // background is most of what says so.
        graphics.fillGradient(0, 0, this.width, this.height, GRASS_TOP, GRASS_BOTTOM);
        for (int i = 0; i < 14; i++) {
            int x = (i * 137) % Math.max(1, this.width);
            int y = 40 + (i * 89) % Math.max(1, this.height - 80);
            graphics.fill(x, y, x + 10, y + 3, 0x22_FFFFFF);
            graphics.fill(x + 3, y - 2, x + 8, y + 1, 0x22_FFFFFF);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (layout == null) {
            return;
        }
        CourseProgress progress = progress();

        drawTopBar(graphics, progress);
        drawPaths(graphics);
        drawNodes(graphics, progress);
        drawSelectionLabel(graphics, progress);
    }

    private void drawTopBar(GuiGraphics graphics, CourseProgress progress) {
        PlaneShiftGui.renderPanel(graphics, 0, 0, this.width, 34);
        WorldDefinition world = world();
        Component heading = Component.translatable("gui.planeshift.course_map.world",
                worldIndex + 1,
                Component.translatableWithFallback(world.nameKey(), world.displayName()));
        graphics.drawString(this.font, heading, 10, 12, PlaneShiftGui.COIN_YELLOW, true);

        CourseState state = state();
        // Lives, coins, star coins — the three counters a Mario map always shows.
        String lives = "♥ " + state.lives();
        String coins = "● " + state.coins();
        String stars = "★ " + progress.totalStarCoins();
        int right = this.width - 10;
        graphics.drawString(this.font, stars, right - this.font.width(stars), 12, 0xFF_FFE873, true);
        right -= this.font.width(stars) + 14;
        graphics.drawString(this.font, coins, right - this.font.width(coins), 12, 0xFF_FFD24A, true);
        right -= this.font.width(coins) + 14;
        graphics.drawString(this.font, lives, right - this.font.width(lives), 12, 0xFF_FF7A7A, true);
    }

    private void drawPaths(GuiGraphics graphics) {
        for (WorldMapLayout.Link link : layout.links()) {
            WorldMapLayout.Node a = layout.node(link.from());
            WorldMapLayout.Node b = layout.node(link.to());
            boolean live = unlocked(a) && unlocked(b);
            drawPath(graphics, nodeX(a), nodeY(a), nodeX(b), nodeY(b), live);
        }
    }

    /**
     * Draws one path as an L: along x, then along y.
     *
     * <p>Right angles rather than diagonals, which is how these maps are always drawn — a
     * diagonal at this scale turns into a staircase of pixels and stops reading as a road.
     */
    private void drawPath(GuiGraphics graphics, int x0, int y0, int x1, int y1, boolean live) {
        int fill = live ? PATH : PATH_LOCKED;
        int edge = live ? PATH_EDGE : PATH_LOCKED;
        int half = PATH_WIDTH / 2;

        int minX = Math.min(x0, x1);
        int maxX = Math.max(x0, x1);
        graphics.fill(minX - half, y0 - half - 1, maxX + half, y0 + half + 1, edge);
        graphics.fill(minX - half, y0 - half, maxX + half, y0 + half, fill);

        int minY = Math.min(y0, y1);
        int maxY = Math.max(y0, y1);
        graphics.fill(x1 - half - 1, minY - half, x1 + half + 1, maxY + half, edge);
        graphics.fill(x1 - half, minY - half, x1 + half, maxY + half, fill);
    }

    private void drawNodes(GuiGraphics graphics, CourseProgress progress) {
        for (int i = 0; i < layout.nodes().size(); i++) {
            WorldMapLayout.Node node = layout.node(i);
            if (node.type() == WorldMapLayout.NodeType.START) {
                continue;
            }
            int x = nodeX(node);
            int y = nodeY(node);
            boolean open = unlocked(node);
            boolean cleared = node.isPlayable() && progress.cleared(node.id());

            if (i == selected) {
                graphics.fill(x - NODE / 2 - 3, y - NODE / 2 - 3,
                        x + NODE / 2 + 3, y + NODE / 2 + 3, 0xFF_FFF3A0);
            }

            switch (node.type()) {
                case TOAD_HOUSE -> drawToadHouse(graphics, x, y, open);
                case CANNON -> drawCannon(graphics, x, y, open);
                case CASTLE -> drawCastle(graphics, x, y, open, cleared);
                default -> drawCourse(graphics, x, y, open, cleared, node.index() + 1);
            }

            if (open && node.isPlayable()) {
                drawStarCoins(graphics, x, y + NODE / 2 + 4, progress.starCoins(node.id()));
            }
        }

        // The player token sits on the selected node, the way Mario stands on the map.
        WorldMapLayout.Node current = layout.node(selected);
        int tx = nodeX(current);
        int ty = nodeY(current) - NODE / 2 - 8;
        graphics.fill(tx - 4, ty - 6, tx + 4, ty + 2, TOKEN);
        graphics.fill(tx - 3, ty - 8, tx + 3, ty - 5, TOKEN);
        graphics.fill(tx - 2, ty + 2, tx + 2, ty + 5, 0xFF_2B4FA8);
    }

    private void drawCourse(GuiGraphics graphics, int x, int y, boolean open, boolean cleared,
                            int number) {
        int fill = !open ? LOCKED : (cleared ? CLEARED : OPEN);
        int edge = !open ? LOCKED_EDGE : (cleared ? CLEARED_EDGE : OPEN_EDGE);
        graphics.fill(x - NODE / 2, y - NODE / 2, x + NODE / 2, y + NODE / 2, edge);
        graphics.fill(x - NODE / 2 + 2, y - NODE / 2 + 2, x + NODE / 2 - 2, y + NODE / 2 - 2, fill);
        String label = open ? Integer.toString(number) : "✖";
        graphics.drawString(this.font, label, x - this.font.width(label) / 2, y - 4,
                0xFF_FFFFFF, true);
    }

    private void drawCastle(GuiGraphics graphics, int x, int y, boolean open, boolean cleared) {
        int fill = !open ? LOCKED : (cleared ? CLEARED : CASTLE);
        int edge = !open ? LOCKED_EDGE : (cleared ? CLEARED_EDGE : CASTLE_EDGE);
        int h = NODE / 2 + 2;
        graphics.fill(x - h, y - h + 4, x + h, y + h, edge);
        graphics.fill(x - h + 2, y - h + 6, x + h - 2, y + h - 2, fill);
        // Crenellations, which is what makes a square read as a castle.
        for (int i = -1; i <= 1; i++) {
            graphics.fill(x + i * 7 - 2, y - h - 1, x + i * 7 + 2, y - h + 5, edge);
        }
        graphics.fill(x - 2, y - 2, x + 2, y + h - 2, edge);
    }

    private void drawToadHouse(GuiGraphics graphics, int x, int y, boolean open) {
        int cap = open ? TOAD_SPOT : LOCKED;
        int body = open ? TOAD : LOCKED_EDGE;
        // Mushroom house: domed red cap with white spots over a pale body.
        graphics.fill(x - 10, y - 8, x + 10, y, cap);
        graphics.fill(x - 8, y - 11, x + 8, y - 7, cap);
        graphics.fill(x - 6, y - 9, x - 3, y - 6, body);
        graphics.fill(x + 3, y - 10, x + 6, y - 7, body);
        graphics.fill(x - 7, y, x + 7, y + 9, body);
        graphics.fill(x - 2, y + 3, x + 2, y + 9, cap);
    }

    private void drawCannon(GuiGraphics graphics, int x, int y, boolean open) {
        int body = open ? CANNON : LOCKED_EDGE;
        int trim = open ? 0xFF_6E7A8A : LOCKED;
        graphics.fill(x - 9, y + 2, x + 9, y + 9, body);
        graphics.fill(x - 5, y - 8, x + 7, y + 3, body);
        graphics.fill(x - 3, y - 10, x + 5, y - 6, trim);
    }

    private void drawStarCoins(GuiGraphics graphics, int x, int y, int found) {
        int pip = 4;
        int spacing = 6;
        int startX = x - (CourseProgress.STAR_COINS_PER_COURSE * spacing) / 2 + 1;
        for (int i = 0; i < CourseProgress.STAR_COINS_PER_COURSE; i++) {
            int px = startX + i * spacing;
            graphics.fill(px, y, px + pip, y + pip,
                    i < found ? PlaneShiftGui.COIN_YELLOW : 0x66_000000);
        }
    }

    private void drawSelectionLabel(GuiGraphics graphics, CourseProgress progress) {
        WorldMapLayout.Node node = layout.node(selected);
        Component line;
        if (!unlocked(node)) {
            line = Component.translatable("gui.planeshift.course_map.locked");
        } else if (node.type() == WorldMapLayout.NodeType.TOAD_HOUSE) {
            line = Component.translatable("gui.planeshift.map.toad_house");
        } else if (node.type() == WorldMapLayout.NodeType.CANNON) {
            line = Component.translatable("gui.planeshift.map.cannon");
        } else {
            line = Component.translatable("gui.planeshift.course_map.detail",
                    node.index() + 1, progress.record(node.id()).bestScore());
        }
        int y = this.height - 48;
        PlaneShiftGui.renderPanel(graphics, this.width / 2 - 150, y - 4, 300, 18);
        graphics.drawString(this.font, line, this.width / 2 - this.font.width(line) / 2, y,
                0xFF_FFFFFF, true);
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        updateArrows();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
