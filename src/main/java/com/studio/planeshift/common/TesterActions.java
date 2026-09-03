package com.studio.planeshift.common;

import java.util.List;

/**
 * The vocabulary of the tester menu, shared by the screen that offers the buttons and the service
 * that executes them.
 *
 * <p>Lives in {@code common} so the two cannot drift apart. The client builds its buttons from
 * these lists and the server resolves them against the same lists, so a key that renders is a key
 * that works — and the server still refuses anything not on the list, because the menu is a
 * convenience for reaching these actions rather than the thing that authorises them.
 */
public final class TesterActions {

    public static final String GIVE = "give";
    public static final String SPAWN = "spawn";
    public static final String COURSE = "course";
    public static final String CLOCK = "clock";
    public static final String SCORE = "score";
    public static final String LIVES = "lives";
    public static final String HEAL = "heal";
    public static final String KILL = "kill";
    public static final String COMPLETE = "complete";
    public static final String LEAVE = "leave";
    public static final String UNLOCK_ALL = "unlock_all";
    public static final String RESET_PROGRESS = "reset_progress";
    public static final String AUTOSCROLL = "autoscroll";

    /** Power-ups and pickups the menu can grant, in the order it shows them. */
    public static final List<String> GRANTS = List.of(
            "super_mushroom", "mega_mushroom", "mini_mushroom",
            "fire_flower", "ice_flower", "leaf",
            "propeller_mushroom", "cloud_flower", "tanooki_suit",
            "hammer", "boomerang", "acorn", "cat_suit",
            "star_power", "poison_mushroom", "extra_pip",
            "three_up", "five_up", "coin", "star_coin");

    /** Entities the menu can spawn, in the order it shows them. */
    public static final List<String> SPAWNS = List.of(
            "goomba", "koopa", "buzzy_beetle",
            "spiny", "lakitu", "boo",
            "thwomp", "hammer_bro", "piranha_plant",
            "bullet_bill", "bowser", "toad");

    private TesterActions() {
    }
}
