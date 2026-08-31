package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseTheme;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the active course theme per player.
 */
public final class CourseThemeService {

    private static final Map<UUID, CourseTheme> PLAYER_THEME = new HashMap<>();

    private CourseThemeService() {
    }

    public static void set(net.minecraft.server.level.ServerPlayer player, CourseTheme theme) {
        PLAYER_THEME.put(player.getUUID(), theme);
    }

    public static CourseTheme get(net.minecraft.server.level.ServerPlayer player) {
        return PLAYER_THEME.getOrDefault(player.getUUID(), CourseTheme.GRASS);
    }

    public static void clear(UUID playerId) {
        PLAYER_THEME.remove(playerId);
    }
}
