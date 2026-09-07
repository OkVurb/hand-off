package com.studio.planeshift.common.course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all worlds and their courses. Each world contains 10 courses,
 * with the 10th always being a boss course (lava/castle theme).
 *
 * <p>World 1: Grassland – grass/desert mix, easy
 * <p>World 2: Frozen Peaks – snow/underground mix, medium
 * <p>World 3: Volcano – lava/underground mix, hard
 * <p>World 4: Haunted Manor – ghost_house/underground mix, hard
 * <p>World 5: Sky Kingdom – mixed themes, expert
 *
 * <p>Lives in {@code common} rather than {@code server} because the world map screen needs the
 * same table and the same unlock rule the server enforces. The rule is a pure function of a
 * {@link CourseProgress}, so the client can grey out a locked course honestly instead of guessing,
 * while the server still refuses the load — see {@code ProgressionService}.
 */
public final class WorldRegistry {

    private static final Map<String, WorldDefinition> WORLDS = new LinkedHashMap<>();
    private static final List<WorldDefinition> ORDERED = new ArrayList<>();

    static {
        register("grassland", "Grassland", CourseTheme.GRASS, 1);
        register("frozen", "Frozen Peaks", CourseTheme.SNOW, 11);
        register("volcano", "Volcano", CourseTheme.LAVA, 21);
        register("haunted", "Haunted Manor", CourseTheme.GHOST_HOUSE, 31);
        register("sky", "Sky Kingdom", CourseTheme.SKY, 41);
    }

    private WorldRegistry() {
    }

    private static void register(String worldId, String displayName, CourseTheme theme, int startIndex) {
        List<String> courseIds = new ArrayList<>();
        for (int i = 1; i <= WorldDefinition.COURSES_PER_WORLD; i++) {
            courseIds.add("w" + (startIndex + i - 1) + "_" + worldId + "_" + i);
        }
        WorldDefinition world = new WorldDefinition(worldId, displayName, theme, Collections.unmodifiableList(courseIds));
        WORLDS.put(worldId, world);
        ORDERED.add(world);
    }

    /** Get all worlds in order. */
    public static List<WorldDefinition> allWorlds() {
        return Collections.unmodifiableList(ORDERED);
    }

    /** Get a world by its ID. */
    public static WorldDefinition get(String worldId) {
        return WORLDS.get(worldId);
    }

    /** Find which world a course belongs to. */
    public static WorldDefinition worldForCourse(String courseId) {
        for (WorldDefinition world : ORDERED) {
            if (world.courseIds().contains(courseId)) {
                return world;
            }
        }
        return null;
    }

    /** Get the world index (0-based) for unlock gating. */
    public static int worldIndex(String worldId) {
        for (int i = 0; i < ORDERED.size(); i++) {
            if (ORDERED.get(i).worldId().equals(worldId)) {
                return i;
            }
        }
        return -1;
    }

    /** Total number of worlds. */
    public static int worldCount() {
        return ORDERED.size();
    }

    /**
     * Whether a saved progress record opens a course.
     *
     * <p>The rules, in order:
     * <ul>
     *   <li>A course this table has never heard of — the five vertical-slice courses, or anything
     *       a datapack adds outside the world list — is always open. Gating unknown content locks
     *       it out with no way in.</li>
     *   <li>A course already cleared stays open, so it can be replayed for star coins.</li>
     *   <li>Any course after the first in a world opens when the previous one is cleared.</li>
     *   <li>The first course of a world opens when the previous world's boss course is cleared.</li>
     * </ul>
     */
    /**
     * Star coins needed before the last world opens.
     *
     * <p>The mod has tracked star coins for the whole life of the codebase and spent them on
     * nothing: three per course, collected, counted, displayed, and gating precisely zero doors.
     * The reference puts its final world behind a count, which is what makes the optional
     * collectable not optional -- a coin you can always skip is decoration, and a player who
     * skipped every one of them was never asked to notice they existed.
     *
     * <p>Sixty is deliberately reachable. There are three coins in each of the fifty courses, so
     * this is sixty of the hundred and twenty available before the final world -- half. A player
     * who has been picking up coins when they are on the way will already have it; a player who
     * has ignored them entirely has four worlds of courses to go back to, and knows exactly what
     * to do. A gate you cannot see how to open is a wall.
     */
    public static final int FINAL_WORLD_STAR_COINS = 60;

    /**
     * How many star coins are still needed before the given world will open.
     *
     * <p>Zero when the world is not gated or the requirement is already met. Exposed so the map
     * screen can say "you need eleven more" rather than greying a node out with no explanation,
     * which is the difference between a goal and a dead end.
     */
    public static int starCoinsStillNeeded(CourseProgress progress, WorldDefinition world) {
        if (!isFinalWorld(world)) {
            return 0;
        }
        return Math.max(0, FINAL_WORLD_STAR_COINS - progress.totalStarCoins());
    }

    /** Whether this is the last world in the run, and therefore the gated one. */
    public static boolean isFinalWorld(WorldDefinition world) {
        return !ORDERED.isEmpty()
                && ORDERED.get(ORDERED.size() - 1).worldId().equals(world.worldId());
    }

    /**
     * Whether clearing one course is what opened the final world.
     *
     * <p>The reference announces structural change -- "Star World has appeared!" -- and the reason
     * is that a lock quietly becoming unlocked is invisible. The player who finally crosses the
     * star-coin threshold is looking at a results screen, not at the map, and without a banner the
     * only evidence is a node that stopped being grey on a screen they may not open for a while.
     *
     * <p>Computed by subtracting the run's own contribution rather than by remembering the
     * previous state, so it needs nothing synced and nothing persisted: if the world is open now
     * and would not have been without this clear, this clear is what opened it.
     */
    public static boolean justOpenedFinalWorld(CourseProgress after, String clearedCourseId,
                                               int starCoinsThisRun) {
        if (ORDERED.isEmpty()) {
            return false;
        }
        WorldDefinition last = ORDERED.get(ORDERED.size() - 1);
        if (!isWorldUnlocked(after, last)) {
            return false;
        }
        // The same test against progress as it stood before this course was cleared.
        // Parenthesised rather than leaning on && binding tighter than ||. The reading is "there
        // is no previous world, or its boss was already cleared and it was not cleared just now".
        boolean bossWasCleared = ORDERED.size() < 2
                || (after.cleared(ORDERED.get(ORDERED.size() - 2).bossCourseId())
                    && !ORDERED.get(ORDERED.size() - 2).bossCourseId().equals(clearedCourseId));
        boolean coinsWereEnough =
                after.totalStarCoins() - starCoinsThisRun >= FINAL_WORLD_STAR_COINS;
        return !(bossWasCleared && coinsWereEnough);
    }

    /**
     * Whether this clear was the one that finished the game.
     *
     * <p>The string for this banner has been in the language file the whole time, spoken by
     * nothing. The reference marks the moment; the mod counted it and said nothing, which is the
     * same shape as the star coins that gated nothing.
     *
     * <p>Everything is cleared now, and this course was not cleared before. The second half is
     * what stops the banner reappearing on every replay of an already-finished game, which would
     * turn a once-in-a-run moment into wallpaper. It cannot be derived from {@code after} -- the
     * clear is already recorded there -- so the caller passes what it knew beforehand, the same
     * shape as {@link #justOpenedFinalWorld} taking the coins earned this run.
     *
     * @param clearedBefore whether the course just finished was already cleared before this run
     */
    public static boolean justClearedEverything(CourseProgress after, boolean clearedBefore) {
        for (WorldDefinition world : ORDERED) {
            for (String courseId : world.courseIds()) {
                if (!after.cleared(courseId)) {
                    return false;
                }
            }
        }
        return !clearedBefore;
    }

    public static boolean isUnlocked(CourseProgress progress, String courseId) {
        return isUnlocked(progress, courseId, false);
    }

    /**
     * The same rule, with an explicit override for players who are exempt from progression.
     *
     * <p>The override is a parameter rather than something the server checks on its own, and that
     * is the whole point of it. A creative bypass added inside ProgressionService made the server
     * permissive while the map screen and MapNodeService went on calling this class directly and
     * kept greying courses out — so a creative player was shown a locked course the server would
     * happily have loaded, and the two halves of the game disagreed about the rules.
     *
     * <p>Keeping the exemption here means every caller computes the same answer from the same
     * function. The client passes its local player's creative flag, the server passes the real
     * one, and the map cannot drift out of step with what loading a course will actually do.
     */
    public static boolean isUnlocked(CourseProgress progress, String courseId,
                                     boolean bypassLocks) {
        if (bypassLocks) {
            return true;
        }
        WorldDefinition world = worldForCourse(courseId);
        if (world == null) {
            return true;
        }
        if (progress.cleared(courseId)) {
            return true;
        }

        List<String> courses = world.courseIds();
        int index = courses.indexOf(courseId);
        if (index > 0) {
            return progress.cleared(courses.get(index - 1));
        }

        int worldIndex = worldIndex(world.worldId());
        if (worldIndex <= 0) {
            return true;
        }
        if (!progress.cleared(ORDERED.get(worldIndex - 1).bossCourseId())) {
            return false;
        }
        // The coin gate sits on top of the boss requirement rather than replacing it, so the
        // final world needs both the previous castle cleared and the coins found. Checked last
        // because clearing the castle is the thing the player is already trying to do.
        return starCoinsStillNeeded(progress, world) == 0;
    }

    /** A world is open when its first course is. Used by the map screen to grey out a page. */
    public static boolean isWorldUnlocked(CourseProgress progress, WorldDefinition world) {
        return isUnlocked(progress, world.courseIds().get(0), false);
    }

    /** A world is open when its first course is, honouring the same exemption. */
    public static boolean isWorldUnlocked(CourseProgress progress, WorldDefinition world,
                                          boolean bypassLocks) {
        return isUnlocked(progress, world.courseIds().get(0), bypassLocks);
    }

    /** Total number of courses across all worlds. */
    public static int totalCourses() {
        return ORDERED.stream().mapToInt(WorldDefinition::courseCount).sum();
    }
}
