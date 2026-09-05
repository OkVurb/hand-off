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
        register("sky", "Sky Kingdom", CourseTheme.GRASS, 41);
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
        return progress.cleared(ORDERED.get(worldIndex - 1).bossCourseId());
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
