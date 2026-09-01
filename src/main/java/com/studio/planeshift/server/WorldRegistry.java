package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.course.WorldDefinition;

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

    /** Total number of courses across all worlds. */
    public static int totalCourses() {
        return ORDERED.stream().mapToInt(WorldDefinition::courseCount).sum();
    }
}
