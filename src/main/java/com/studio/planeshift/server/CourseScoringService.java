package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks course start times and computes a score on completion.
 */
public final class CourseScoringService {

    private static final Map<UUID, Long> COURSE_START = new HashMap<>();

    private CourseScoringService() {
    }

    public static void startCourse(ServerPlayer player) {
        COURSE_START.put(player.getUUID(), player.level().getGameTime());
    }

    public static void finishCourse(ServerPlayer player) {
        Long start = COURSE_START.remove(player.getUUID());
        long ticks = start != null ? player.level().getGameTime() - start : 0L;
        CourseState state = CourseStateAccess.get(player);

        // Mario-style scoring: coins, star coins, pips, and speed bonus.
        long score = state.coins() * 100L
                + state.starCoins() * 1000L
                + state.pips() * 500L
                - ticks * 2L;
        score = Math.max(0, score);

        player.sendSystemMessage(Component.translatable("chat.planeshift.course_complete",
                state.coins(), state.starCoins(), formatTime(ticks), score));
    }

    public static String formatTime(long ticks) {
        int totalSeconds = (int) (ticks / 20L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        int centi = (int) ((ticks % 20L) * 5L);
        return String.format("%d:%02d.%02d", minutes, seconds, centi);
    }

    public static void clear(UUID playerId) {
        COURSE_START.remove(playerId);
    }
}
