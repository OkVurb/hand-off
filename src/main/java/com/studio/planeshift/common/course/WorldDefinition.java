package com.studio.planeshift.common.course;

import java.util.List;

/**
 * Defines a world (zone) containing multiple courses. Each world has a name,
 * a list of course IDs that belong to it, and a theme that sets the visual tone.
 */
public record WorldDefinition(
        String worldId,
        String displayName,
        CourseTheme primaryTheme,
        List<String> courseIds
) {
    /** Number of courses per world. */
    public static final int COURSES_PER_WORLD = 10;

    public int courseCount() {
        return courseIds.size();
    }

    /** The last course in each world is always a boss course (lava/castle). */
    public String bossCourseId() {
        return courseIds.get(courseIds.size() - 1);
    }

    /** Check if a specific course has an underwater second stage (~5% of courses). */
    public boolean hasUnderwaterStage(String courseId) {
        int index = courseIds.indexOf(courseId);
        // ~5% = roughly 1 in 20. Courses at index 2 in worlds 0 and 2 get it.
        return index == 2 && (worldId.equals("grassland") || worldId.equals("volcano"));
    }
}
