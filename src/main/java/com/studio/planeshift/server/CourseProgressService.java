package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public final class CourseProgressService {
    private static final Map<ServerPlayer, ServerBossEvent> BARS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> START_X = new WeakHashMap<>();

    private CourseProgressService() {}

    public static void tick(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            removeBar(player);
            return;
        }

        int playerX = (int) player.getX();
        START_X.putIfAbsent(player, playerX);
        int startX = START_X.get(player);

        ServerBossEvent bar = BARS.computeIfAbsent(player, p -> {
            ServerBossEvent b = new ServerBossEvent(Component.translatable("hud.planeshift.course_progress"), 
                BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
            b.addPlayer(p);
            return b;
        });

        int distance = Math.max(0, playerX - startX);
        float progress = Math.min(1.0F, distance / 120.0F); // Assuming ~120 block course length
        
        bar.setProgress(progress);
        bar.setName(Component.translatable("hud.planeshift.course_progress.player",
                player.getScoreboardName()));
    }

    public static void removeBar(ServerPlayer player) {
        ServerBossEvent bar = BARS.remove(player);
        if (bar != null) {
            bar.removePlayer(player);
        }
        START_X.remove(player);
    }
}
