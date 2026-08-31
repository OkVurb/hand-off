package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;

/**
 * The course clock (Design Bible, "Course rules"): a per-player countdown that ends the run
 * when it reaches zero.
 *
 * <p>The remaining time lives in {@link CourseState} rather than a map here, because the HUD
 * has to render it and the value has to survive a checkpoint return. This class only advances
 * it and reacts when it hits zero.
 *
 * <p>Counted in ticks but only written once per second. Rewriting the attachment 20 times a
 * second would resync the whole state to the client every tick for a number that changes at
 * 1 Hz.
 */
public final class CourseTimerService {

    /** Ticks between clock updates. One second, matching what the HUD displays. */
    private static final int TICK_INTERVAL = 20;

    private CourseTimerService() {
    }

    /**
     * Advances the clock for one player. Safe to call every tick.
     *
     * <p>Does nothing outside a course, on an untimed course, or while a transition is in
     * flight — the clock should not drain during a mode shift the player cannot act through.
     */
    public static void tick(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse() || !state.timed() || state.transition().isPresent()) {
            return;
        }
        if (state.timeExpired()) {
            return;
        }
        // Phase off the player's own game time so every player is not written on the same tick.
        long now = player.level().getGameTime();
        if (now % TICK_INTERVAL != 0L) {
            return;
        }

        boolean wasCritical = state.timeCritical();
        int remaining = Math.max(0, state.timeLeft() - TICK_INTERVAL);
        CourseStateAccess.update(player, s -> s.withTimeLeft(remaining));

        if (!wasCritical && remaining <= CourseState.TIME_WARNING_TICKS && remaining > 0) {
            // One-shot warning as the clock crosses into the danger band.
            player.level().playSound(null, player.blockPosition(), ModSounds.QUESTION_BUMP.get(),
                    SoundSource.PLAYERS, 0.9F, 1.8F);
        }

        if (remaining <= 0) {
            expire(player);
        }
    }

    /**
     * Time up. Costs a life through the ordinary damage path so the checkpoint, life counter
     * and game-over handling stay in one place rather than being duplicated here.
     */
    private static void expire(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), ModSounds.GAME_OVER.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        DamageSource source = player.damageSources().generic();
        DamageService.down(player, source);
    }

    /** Starts the clock for a course, or clears it when the course is untimed. */
    public static void start(ServerPlayer player, int timeLimitTicks, boolean autoScroll) {
        CourseStateAccess.update(player, s -> s.withCourseRules(timeLimitTicks, autoScroll));
    }

    /** Clears the clock and auto-scroll rule, for leaving a course or returning to the hub. */
    public static void clear(ServerPlayer player) {
        CourseStateAccess.update(player, s -> s.withCourseRules(CourseState.NO_TIME_LIMIT, false));
    }
}
