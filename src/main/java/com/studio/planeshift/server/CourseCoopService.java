package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Co-op helpers: share certain beneficial pickups with nearby party members.
 */
public final class CourseCoopService {

    private static final double SHARE_RADIUS = 32.0D;

    private CourseCoopService() {
    }

    public static void shareLives(ServerPlayer source, int lives) {
        CourseState state = CourseStateAccess.get(source);
        if (!state.inCourse()) {
            return;
        }
        AABB box = source.getBoundingBox().inflate(SHARE_RADIUS);
        for (Entity entity : source.level().getEntities(source, box, e -> e instanceof ServerPlayer && e != source)) {
            ServerPlayer other = (ServerPlayer) entity;
            CourseState otherState = CourseStateAccess.get(other);
            if (!otherState.inCourse()) {
                continue;
            }
            CourseStateAccess.update(other, s -> s.withLives(s.lives() + lives));
        }
    }
}
