package com.studio.planeshift.client.screen;

import com.studio.planeshift.client.gui.PlaneShiftGui;
import com.studio.planeshift.common.TesterActions;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.network.TesterActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The playtest tester menu (F6).
 *
 * <p>Exists so a feel problem can be reproduced in seconds rather than by playing up to it. Most
 * of what is worth testing here — the hurry-up music, the game-over screen, unlock gating, a
 * specific power-up's particles — sits behind several minutes of play, which is long enough that
 * it does not get tested.
 *
 * <p>Every button is a request, not an action: the server owns the whole vocabulary and refuses
 * anything not on its list, so this screen cannot do more than {@code TesterService} allows.
 */
public class TesterScreen extends Screen {

    /** Tabs, in the order they appear. */
    private enum Tab {
        POWER_UPS("power_ups"),
        SPAWN("spawn"),
        COURSE("course"),
        STATE("state"),
        PROGRESS("progress");

        final String key;

        Tab(String key) {
            this.key = key;
        }

        Component label() {
            return Component.translatable("gui.planeshift.tester.tab." + key);
        }
    }

    private static final int COLUMNS = 4;
    private static final int CELL_WIDTH = 96;
    private static final int CELL_HEIGHT = 20;
    private static final int CELL_GAP = 4;
    private static final int TAB_HEIGHT = 18;

    private Tab tab = Tab.POWER_UPS;
    private final List<Button> tabButtons = new ArrayList<>();

    public TesterScreen() {
        super(Component.translatable("gui.planeshift.tester"));
    }

    @Override
    protected void init() {
        tabButtons.clear();

        int tabWidth = Math.min(84, (this.width - 20) / Tab.values().length);
        int tabsLeft = this.width / 2 - (tabWidth * Tab.values().length) / 2;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab value = Tab.values()[i];
            Button button = Button.builder(value.label(), b -> {
                        tab = value;
                        rebuild();
                    })
                    .bounds(tabsLeft + i * tabWidth, 28, tabWidth - 2, TAB_HEIGHT)
                    .build();
            tabButtons.add(button);
            addRenderableWidget(button);
        }

        buildGrid();

        addRenderableWidget(Button.builder(Component.translatable("gui.planeshift.tester.close"),
                        b -> this.onClose())
                .bounds(this.width / 2 - 60, this.height - 26, 120, 20)
                .build());

        updateTabState();
    }

    /** Re-runs {@link #init()} so the grid matches the selected tab. */
    private void rebuild() {
        this.rebuildWidgets();
    }

    private void updateTabState() {
        for (int i = 0; i < tabButtons.size(); i++) {
            // The active tab is the one you cannot press, which is the cheapest possible
            // "you are here" without a second widget type.
            tabButtons.get(i).active = Tab.values()[i] != tab;
        }
    }

    private void buildGrid() {
        List<Entry> entries = entriesFor(tab);
        int top = 56;
        int gridWidth = COLUMNS * CELL_WIDTH + (COLUMNS - 1) * CELL_GAP;
        int left = this.width / 2 - gridWidth / 2;

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int y = top + row * (CELL_HEIGHT + CELL_GAP);
            if (y + CELL_HEIGHT > this.height - 34) {
                break;   // never draw a button under the Close button
            }
            addRenderableWidget(Button.builder(entry.label(),
                            b -> send(entry.action(), entry.arg()))
                    .bounds(left + column * (CELL_WIDTH + CELL_GAP), y, CELL_WIDTH, CELL_HEIGHT)
                    .build());
        }
    }

    /** One button: what it says, and what it asks the server to do. */
    private record Entry(Component label, String action, String arg) {
    }

    private static Entry named(String translationKey, String action, String arg) {
        return new Entry(Component.translatable(translationKey), action, arg);
    }

    private static List<Entry> entriesFor(Tab tab) {
        List<Entry> entries = new ArrayList<>();
        switch (tab) {
            case POWER_UPS -> TesterActions.GRANTS.forEach(key -> entries.add(
                    named("item.planeshift." + key, TesterActions.GIVE, key)));
            case SPAWN -> TesterActions.SPAWNS.forEach(key -> entries.add(
                    named("entity.planeshift." + key, TesterActions.SPAWN, key)));
            case COURSE -> {
                // The five vertical-slice courses, then the first course of each world. Listing
                // all fifty would need its own scroll pane; the world map already does that job.
                for (int i = 1; i <= 5; i++) {
                    entries.add(named("course.planeshift.course_" + i, TesterActions.COURSE,
                            "course_" + i));
                }
                for (WorldDefinition world : WorldRegistry.allWorlds()) {
                    String first = world.courseIds().get(0);
                    entries.add(named("course.planeshift." + first, TesterActions.COURSE, first));
                    String boss = world.bossCourseId();
                    entries.add(named("course.planeshift." + boss, TesterActions.COURSE, boss));
                }
            }
            case STATE -> {
                entries.add(named("gui.planeshift.tester.heal", TesterActions.HEAL, ""));
                entries.add(named("gui.planeshift.tester.kill", TesterActions.KILL, ""));
                entries.add(named("gui.planeshift.tester.lives_1", TesterActions.LIVES, "1"));
                entries.add(named("gui.planeshift.tester.lives_3", TesterActions.LIVES, "3"));
                // 10 seconds is under the 100-second warning band, so this is the fastest way to
                // hear the hurry-up music and watch the clock flash.
                entries.add(named("gui.planeshift.tester.clock_10", TesterActions.CLOCK, "10"));
                entries.add(named("gui.planeshift.tester.clock_100", TesterActions.CLOCK, "100"));
                entries.add(named("gui.planeshift.tester.clock_400", TesterActions.CLOCK, "400"));
                entries.add(named("gui.planeshift.tester.autoscroll", TesterActions.AUTOSCROLL, ""));
                entries.add(named("gui.planeshift.tester.score", TesterActions.SCORE, "5000"));
                entries.add(named("gui.planeshift.tester.complete", TesterActions.COMPLETE, ""));
                entries.add(named("gui.planeshift.tester.leave", TesterActions.LEAVE, ""));
            }
            case PROGRESS -> {
                entries.add(named("gui.planeshift.tester.unlock_all", TesterActions.UNLOCK_ALL, ""));
                entries.add(named("gui.planeshift.tester.reset_progress",
                        TesterActions.RESET_PROGRESS, ""));
            }
        }
        return entries;
    }

    /**
     * Sends the request and closes.
     *
     * <p>Closing matters: almost everything here is only observable in the world — particles, the
     * music change, the game-over screen — and none of it can be seen through a full-screen menu.
     */
    private void send(String action, String arg) {
        ClientPacketDistributor.sendToServer(new TesterActionPayload(action, arg));
        if (!action.equals(TesterActions.GIVE) && !action.equals(TesterActions.SPAWN)) {
            this.onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlaneShiftGui.renderThemedBackground(graphics, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        PlaneShiftGui.drawTitle(graphics, this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, 10, PlaneShiftGui.COIN_YELLOW);
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        updateTabState();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
