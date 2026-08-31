package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModAttachments;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;

/**
 * The single write path for {@link CourseState}.
 *
 * <p>Every mutation goes through {@link #update}, which stores the new snapshot and
 * syncs it to tracking clients. Services never touch the attachment directly, so there
 * is exactly one owner per transition — the invariant the state model requires.
 */
public final class CourseStateAccess {

    private CourseStateAccess() {
    }

    public static CourseState get(ServerPlayer player) {
        return player.getData(ModAttachments.COURSE_STATE);
    }

    public static CourseState update(ServerPlayer player, UnaryOperator<CourseState> mutation) {
        CourseState previous = player.getData(ModAttachments.COURSE_STATE);
        CourseState next = mutation.apply(previous);
        if (!next.equals(previous)) {
            player.setData(ModAttachments.COURSE_STATE, next);
            player.syncData(ModAttachments.COURSE_STATE.get());
        }
        return next;
    }
}
