package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.registry.ModEffects;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * The course damage model (Design Bible, "Combat, stomp, damage, and recovery"):
 * "Two health pips plus an active Form buffer: a normal hit removes the Form first,
 * then one pip. Grant 1.25 seconds of invulnerability after damage."
 *
 * <p>Star Power makes the player fully invincible (except pits and /kill).
 * <p>A lethal hit (0 pips) consumes a life; at 0 lives the player dies for real.
 * <p>Inside a course, vanilla health is left untouched: the pip model intercepts
 * incoming damage entirely. Outside courses (HUB and vanilla play), damage passes
 * through unchanged.
 */
public final class DamageService {

    /** 1.25 s at 20 TPS. */
    public static final int INVULN_TICKS = 25;

    private DamageService() {
    }

    /**
     * @return true when the pip model consumed the damage (cancel the vanilla event)
     */
    public static boolean interceptDamage(ServerPlayer player, DamageSource source) {
        // Star Power invincibility: ignore most damage, but still allow pits and operator /kill.
        if (player.hasEffect(ModEffects.STAR_POWER) && !source.is(DamageTypes.GENERIC_KILL)
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return true;
        }

        CourseState state = CourseStateAccess.get(player);
        if (state.state() == PlayState.RESULTS) {
            return true;
        }
        if (!state.inCourse()) {
            return false;
        }
        // Operator kills and world-boundary damage stay vanilla so admins keep control.
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }

        long now = player.level().getGameTime();
        if (state.invulnerable(now)) {
            return true;
        }

        // Any hit invalidates an in-flight perspective shift before state changes.
        ModeTransitionService.abortIfActive(player);

        if (FormService.absorbHitWithForm(player)) {
            CourseStateAccess.update(player, s -> s.withPips(s.pips(), now + INVULN_TICKS));
            return true;
        }

        int remaining = state.pips() - 1;
        if (remaining <= 0) {
            down(player, source);
        } else {
            CourseStateAccess.update(player, s -> s.withPips(remaining, now + INVULN_TICKS));
            player.level().playSound(null, player.blockPosition(), ModSounds.DAMAGE.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }

    public static final java.util.Map<java.util.UUID, Long> DOWN_TIMES = new java.util.HashMap<>();

    /** DOWNED -> sets state to DOWNED and triggers pop-up animation. */
    public static void down(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            resolveDown(player);
            return;
        }

        long now = player.level().getGameTime();
        CourseStateAccess.update(player, s -> s
                .withState(PlayState.DOWNED)
                .withPips(s.pips(), now + 100L));
        DOWN_TIMES.put(player.getUUID(), now);

        player.setDeltaMovement(0.0, 0.7, 0.0);
        player.hurtMarked = true;
        
        player.level().playSound(null, player.blockPosition(), ModSounds.DAMAGE.get(),
                SoundSource.PLAYERS, 0.8F, 0.9F);
    }

    /** Called after the DOWNED animation finishes, or immediately if falling out of the world. */
    public static void resolveDown(ServerPlayer player) {
        long now = player.level().getGameTime();
        CourseState state = CourseStateAccess.get(player);
        int newLives = Math.max(0, state.lives() - 1);

        if (newLives == 0) {
            // Game over: the run is finished, so the player leaves the course entirely rather
            // than respawning inside it with a fresh life bar. Returning to the hub also clears
            // the course clock and auto-scroll rule.
            player.level().playSound(null, player.blockPosition(), ModSounds.GAME_OVER.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            CourseStateAccess.update(player, s -> s
                    .withState(PlayState.HUB)
                    .withMode(PlaneMode.FREE_3D, Optional.empty())
                    .withFormSlot(s.formSlot().loseActive())
                    .withPips(1, now + INVULN_TICKS * 2L)
                    .withLives(CourseState.STARTING_LIVES)
                    .withCheckpoint(Optional.empty()));
            // Read before the reset below wipes them; the screen has nothing else to show.
            String courseId = ProgressionService.get(player).currentCourse().orElse("");
            int finalScore = state.score();

            CourseScoringService.clear(player.getUUID());
            CourseService.returnToHub(player);
            // The run is over, so the player is no longer in any course. Without this a star coin
            // picked up later would still be credited to the course they just lost.
            ProgressionService.leaveCourse(player);
            player.sendSystemMessage(Component.translatable("chat.planeshift.game_over"));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new com.studio.planeshift.common.network.GameOverPayload(courseId, finalScore));
            return;
        }

        CourseStateAccess.update(player, s -> s
                .withState(PlayState.playingFor(s.mode())) // Return to playing state
                .withFormSlot(s.formSlot().loseActive())
                .withPips(CourseState.MAX_PIPS, now + INVULN_TICKS * 2L)
                .withLives(newLives));
        CheckpointService.returnToCheckpoint(player);
    }
}
