package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.network.OpenCourseMapPayload;
import com.studio.planeshift.common.network.OpenTitleScreenPayload;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles course completion: rewards, state cleanup, and returning to the map.
 *
 * <p>This is intentionally small for the vertical slice. Later it will record best
 * times, unlock next courses, and teleport to a hub dimension.
 */
public final class CourseCompletionService {

    private CourseCompletionService() {
    }

    public static void onComplete(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }

        CourseService.returnToHub(player);

        // Reward: coins scaled by pips remaining (placeholder formula).
        CourseStateAccess.update(player, s -> s
                .withState(PlayState.HUB)
                .withMode(PlaneMode.FREE_3D, Optional.empty())
                .withPips(CourseState.MAX_PIPS, 0L)
                .withCoins(s.coins() + Math.max(1, s.pips() * 2)));

        CourseScoringService.finishCourse(player);
        player.level().playSound(null, player.blockPosition(), ModSounds.COURSE_CLEAR.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.sendParticles(ModParticles.PICKUP_GLOW.get(),
                    player.getX(), player.getY(0.5D), player.getZ(),
                    16, 0.4D, 0.4D, 0.4D, 0.08D);
        }
        PacketDistributor.sendToPlayer(player, OpenTitleScreenPayload.INSTANCE);
    }
}
