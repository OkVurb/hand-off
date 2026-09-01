package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseProgress;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.registry.ModAttachments;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;

/**
 * The save file and the unlock rules.
 *
 * <p>Two responsibilities that belong together because they are the same question asked in two
 * directions: what has this player done, and what does that let them do next.
 *
 * <p>Unlock gating is enforced here, on the server, not in the map screen. The screen greys out
 * locked courses because that is honest UI, but a client that sends a {@code CourseSelectPayload}
 * for a locked course is refused — the screen is a convenience, never the authority.
 */
public final class ProgressionService {

    private ProgressionService() {
    }

    public static CourseProgress get(ServerPlayer player) {
        return player.getData(ModAttachments.COURSE_PROGRESS);
    }

    public static CourseProgress update(ServerPlayer player, UnaryOperator<CourseProgress> mutation) {
        CourseProgress previous = get(player);
        CourseProgress next = mutation.apply(previous);
        if (!next.equals(previous)) {
            player.setData(ModAttachments.COURSE_PROGRESS, next);
            player.syncData(ModAttachments.COURSE_PROGRESS.get());
        }
        return next;
    }

    /** Called as a course loads, so later events know what they are attributing progress to. */
    public static void enterCourse(ServerPlayer player, String courseId) {
        update(player, p -> p.withCurrentCourse(Optional.of(courseId)));
    }

    public static void leaveCourse(ServerPlayer player) {
        update(player, p -> p.withCurrentCourse(Optional.empty()));
    }

    /**
     * Records a clear against whichever course the player is actually in.
     *
     * <p>A player outside a course cannot clear one, which sounds obvious but is the reason this
     * reads {@code currentCourse} rather than taking an id from the caller: the flagpole knows a
     * position, not a course id, and guessing from position is how a player ends up credited for
     * the neighbouring course 256 blocks away.
     */
    public static void recordClear(ServerPlayer player, int score, int timeLeft) {
        String courseId = get(player).currentCourse().orElse(null);
        if (courseId == null) {
            return;
        }
        update(player, p -> p.withClear(courseId, score, timeLeft));
    }

    /** Credits a star coin to the course the player is in. Capped inside {@link CourseProgress}. */
    public static void recordStarCoin(ServerPlayer player) {
        String courseId = get(player).currentCourse().orElse(null);
        if (courseId == null) {
            return;
        }
        update(player, p -> p.withStarCoin(courseId));
    }

    /**
     * Whether a player may load a course. The rule itself lives in {@link WorldRegistry} so the
     * map screen can grey out the same courses this refuses, from the same data.
     */
    public static boolean isUnlocked(ServerPlayer player, String courseId) {
        return WorldRegistry.isUnlocked(get(player), courseId);
    }

    /** A world is open when its first course is. */
    public static boolean isWorldUnlocked(ServerPlayer player, WorldDefinition world) {
        return WorldRegistry.isWorldUnlocked(get(player), world);
    }
}
