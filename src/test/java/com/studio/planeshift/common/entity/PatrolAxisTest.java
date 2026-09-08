package com.studio.planeshift.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A lane patroller walks along the lane.
 *
 * <p>It used to take its heading from whatever yaw it was spawned with, and seven spawns in the
 * segment library pass a yaw of zero — which is south, across a corridor three blocks deep. Those
 * enemies set off sideways, hit the wall after one block, turned, hit the other wall, and spent
 * their lives oscillating in a one-block space. They never patrolled, and a group of them ends up
 * in a heap.
 *
 * <p>Checked at the source rather than by running a goal, which needs a level and a mob. What
 * matters is that the axis is decided by the goal and not inherited from a spawn yaw, so the check
 * is that the seeding call goes through the corrector.
 */
class PatrolAxisTest {

    private static final Path GOAL = findRoot().resolve(
            "src/main/java/com/studio/planeshift/common/entity/LanePatrolGoal.java");

    private static Path findRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("tools"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find repo root");
        }
        return dir;
    }

    @Test
    @DisplayName("the patrol axis is decided by the goal, not by the spawn yaw")
    void headingIsCorrected() throws IOException {
        String source = Files.readString(GOAL, StandardCharsets.UTF_8);
        assertTrue(source.contains("lane = alongTheLane(mob.getDirection())"),
                "the goal seeds its heading straight from the spawn yaw again; a mob spawned "
                        + "facing across the corridor will oscillate in place");
        assertTrue(source.contains("spawned.getAxis() == Direction.Axis.X"),
                "the corrector no longer keeps east-west headings, so it is not correcting onto "
                        + "the lane axis");
    }

    @Test
    @DisplayName("and north and south are not lane directions")
    void theLaneRunsEastWest() {
        // The lane is three blocks deep on Z and hundreds long on X. Anything that walks Z hits a
        // wall within one block, which is the whole failure this guards.
        assertEquals(Direction.Axis.Z, Direction.NORTH.getAxis());
        assertEquals(Direction.Axis.Z, Direction.SOUTH.getAxis());
        assertEquals(Direction.Axis.X, Direction.EAST.getAxis());
        assertEquals(Direction.Axis.X, Direction.WEST.getAxis());
    }
}
