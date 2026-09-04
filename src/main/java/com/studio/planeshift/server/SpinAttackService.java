package com.studio.planeshift.server;

import com.studio.planeshift.common.entity.CourseEnemyEntity;
import com.studio.planeshift.common.entity.KoopaEntity;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/**
 * The spin's ground attack.
 *
 * <p>The spin already existed and did exactly one thing: {@code CourseEnemyEntity.resolveStomp}
 * read {@code AirMoveService.isSpinJumping} to turn a spike-damage stomp into a safe bounce.
 * Nothing else in the mod consulted it. That is a lot of machinery — an edge-triggered input, a
 * synced state map, a sound — for a single defensive special case.
 *
 * <p>This makes it a verb the player can aim. Starting a spin on the ground sweeps everything
 * beside you: ordinary enemies are hit and knocked off their feet, and a resting Koopa shell is
 * kicked, which is the interaction that turns a spin into a way of *aiming* a shell rather than
 * having to walk into one and hope.
 *
 * <p>Reach is short on purpose. A spin that cleared a three-block circle would make the stomp — the
 * genre's central verb — optional, and every enemy placement in {@code SegmentLibrary} is built
 * around the player having to commit to a jump.
 */
public final class SpinAttackService {

    /** Horizontal reach, in blocks. Shorter than the ground pound's stagger: this one is free. */
    public static final double REACH = 1.6D;

    /** Vertical band. A spin is a waist-height sweep, not a column. */
    public static final double VERTICAL_REACH = 1.2D;

    /** Damage to an ordinary enemy. Enough to finish a Goomba, not enough to trivialise a Koopa. */
    private static final float DAMAGE = 4.0F;

    /** How long a spun enemy is knocked off its feet. Brief — this is an opening, not a stun. */
    private static final int STAGGER_TICKS = 12;

    private SpinAttackService() {
    }

    /**
     * Whether something at this offset is inside the sweep.
     *
     * <p>Pulled out as pure arithmetic for the same reason {@code GroundPoundResolver} is: reach
     * bugs are invisible. A spin measured as a square instead of a circle reaches 41% further at
     * the corners and nobody notices until enemy placement stops meaning anything.
     */
    public static boolean inReach(double dx, double dy, double dz) {
        if (Math.abs(dy) > VERTICAL_REACH) {
            return false;
        }
        return dx * dx + dz * dz <= REACH * REACH;
    }

    /** Sweeps everything beside the player. Returns how many things it touched. */
    public static int strike(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(REACH, VERTICAL_REACH, REACH);
        int hits = 0;

        for (CourseEnemyEntity enemy : level.getEntitiesOfClass(CourseEnemyEntity.class, box,
                CourseEnemyEntity::isAlive)) {
            if (enemy.fallingFromDrop()
                    || !inReach(enemy.getX() - player.getX(),
                                enemy.getY() - player.getY(),
                                enemy.getZ() - player.getZ())) {
                continue;
            }

            // A resting shell is kicked rather than hit. Kicking is strictly more useful to the
            // player than another point of damage, and it is the reason to spin next to one.
            if (enemy instanceof KoopaEntity koopa && koopa.kickFromSpin(player)) {
                hits++;
                continue;
            }

            enemy.invulnerableTime = 0;
            enemy.hurtServer(level, player.damageSources().playerAttack(player), DAMAGE);
            if (enemy.isAlive()) {
                enemy.stagger(STAGGER_TICKS);
            } else {
                CourseScoringService.awardStomp(player);
            }
            hits++;
        }

        if (hits > 0) {
            level.sendParticles(ModParticles.HIT_BURST.get(),
                    player.getX(), player.getY() + 0.6D, player.getZ(), 8, 0.6D, 0.2D, 0.6D, 0.02D);
            level.playSound(null, player.blockPosition(), ModSounds.STOMP.get(),
                    SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        return hits;
    }
}
