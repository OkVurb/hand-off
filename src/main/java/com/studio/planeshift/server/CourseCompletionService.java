package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseProgress;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.network.CourseResultsPayload;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles course completion: scoring the finish, writing the save record, and showing results.
 *
 * <p>Order matters here. The score and the remaining clock are read and recorded before the state
 * is reset, because resetting is what wipes them — recording afterwards would save a zero.
 */
public final class CourseCompletionService {

    private CourseCompletionService() {
    }

    public static void onComplete(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }

        String courseId = ProgressionService.get(player).currentCourse().orElse("");
        int previousBest = ProgressionService.get(player).record(courseId).bestScore();

        // Bonuses are added to the running score, so this must happen before the state reset.
        CourseScoringService.Results results = CourseScoringService.finishCourse(player);
        int timeLeft = Math.max(0, state.timeLeft());
        ProgressionService.recordClear(player, results.finalScore(), timeLeft);

        CourseService.returnToHub(player);

        // Reward: coins scaled by pips remaining (placeholder formula).
        CourseStateAccess.update(player, s -> s
                .withState(PlayState.RESULTS)
                .withMode(PlaneMode.FREE_3D, Optional.empty())
                .withPips(CourseState.MAX_PIPS, 0L)
                .withCoins(s.coins() + Math.max(1, s.pips() * 2)));

        player.level().playSound(null, player.blockPosition(), ModSounds.COURSE_CLEAR.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.sendParticles(ModParticles.PICKUP_GLOW.get(),
                    player.getX(), player.getY(0.5D), player.getZ(),
                    16, 0.4D, 0.4D, 0.4D, 0.08D);
        }

        CourseProgress progress = ProgressionService.get(player);
        PacketDistributor.sendToPlayer(player, new CourseResultsPayload(
                courseId,
                results.finalScore(),
                timeLeft,
                results.timeBonus(),
                state.coins(),
                progress.starCoins(courseId),
                state.lives(),
                results.finalScore() > previousBest));

        ProgressionService.leaveCourse(player);
    }
}
