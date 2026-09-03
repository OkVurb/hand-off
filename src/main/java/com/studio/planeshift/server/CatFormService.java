package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.entity.CourseEnemyEntity;
import com.studio.planeshift.common.registry.ModEffects;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Cat Suit's two movement abilities: the pounce dive and the wall cling.
 *
 * <p>Deliberately only these two. The all-fours run and the wall climb are handled far better by
 * Player Animation Library and ParCool — see {@code docs/THREE_D_AND_ANIMATION.md} — and a second
 * implementation would fight theirs rather than add anything. What is left here is the part that
 * touches systems this mod owns: the dive interacts with the enemy stomp contract in
 * {@link CourseEnemyEntity}, and the cling has to respect the 2.5D rail.
 *
 * <p>The design idea behind both: the Cat Suit converts <em>height</em> into <em>offence</em>.
 * Every other Form turns a button press into a projectile. This one turns a fall into a weapon,
 * which is a different verb and gives the suit an identity beyond "the climbing one".
 */
public final class CatFormService {

    /** Downward speed once a dive commits. Faster than a ground pound; it is an attack. */
    private static final double DIVE_SPEED = -1.45D;
    /** Radius the dive damages on landing. */
    private static final double DIVE_RADIUS = 2.4D;
    /** Damage at the centre of a dive landing. */
    private static final float DIVE_DAMAGE = 14.0F;
    /** Upward bounce after a successful dive, so a hit chains into the next one. */
    private static final double DIVE_REBOUND = 0.62D;

    /** Descent speed while clinging to a wall. Slower than the ordinary wall slide. */
    private static final double CLING_FALL_SPEED = -0.06D;
    /** How long a cling can be held before the player slides off. */
    private static final int CLING_MAX_TICKS = 50;

    private static final Map<ServerPlayer, Boolean> DIVING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> CLING_TICKS = new WeakHashMap<>();

    private CatFormService() {
    }

    /** Whether this player currently has the Cat Suit active. */
    private static boolean hasCatForm(ServerPlayer player) {
        return player.hasEffect(ModEffects.CAT_AURA);
    }

    /** Called once per player tick from {@code ServerEvents}. Cheap when the suit is not worn. */
    public static void tick(ServerPlayer player) {
        if (!hasCatForm(player) || !CourseStateAccess.get(player).inCourse()) {
            forget(player);
            return;
        }

        tickDive(player);
        tickCling(player);
    }

    /**
     * The pounce dive: crouch while airborne to slam downward, damaging everything you land on.
     *
     * <p>Shares its input with the ground pound on purpose. A player who already knows the pound
     * gets the dive for free, and the suit changes what the input <em>means</em> rather than adding
     * a control to remember. The rebound is what separates them: a pound ends on the floor, a dive
     * bounces so a chain of them is possible, which is the fantasy.
     */
    private static void tickDive(ServerPlayer player) {
        boolean airborne = !player.onGround();
        boolean crouching = player.isShiftKeyDown();

        if (airborne && crouching && player.getDeltaMovement().y <= 0.2D) {
            if (!Boolean.TRUE.equals(DIVING.get(player))) {
                DIVING.put(player, true);
                player.level().playSound(null, player.blockPosition(), ModSounds.STOMP.get(),
                        SoundSource.PLAYERS, 0.7F, 1.6F);
            }
            player.setDeltaMovement(player.getDeltaMovement().x * 0.6D, DIVE_SPEED,
                    player.getDeltaMovement().z * 0.6D);
            player.hurtMarked = true;
            player.resetFallDistance();
            trail(player);
            return;
        }

        if (!airborne && Boolean.TRUE.equals(DIVING.remove(player))) {
            land(player);
        }
    }

    /** Resolves a completed dive: damage around the impact, then rebound. */
    private static void land(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB box = player.getBoundingBox().inflate(DIVE_RADIUS, 1.2D, DIVE_RADIUS);
        boolean hitAnything = false;
        for (CourseEnemyEntity enemy : level.getEntitiesOfClass(CourseEnemyEntity.class, box)) {
            if (!enemy.isAlive() || enemy.fallingFromDrop()) {
                continue;
            }
            // Routed through the ordinary attack path so armoured enemies, fire immunity and the
            // squish framework all behave exactly as they do for any other hit.
            enemy.hurtServer(level, player.damageSources().playerAttack(player), DIVE_DAMAGE);
            enemy.startSquish();
            hitAnything = true;
            if (!enemy.isAlive()) {
                CourseScoringService.awardStomp(player);
            }
        }

        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.1D, player.getZ(), 16, 0.9D, 0.05D, 0.9D, 0.04D);
        level.playSound(null, player.blockPosition(),
                hitAnything ? ModSounds.ENEMY_DEFEAT.get() : ModSounds.BRICK_BREAK.get(),
                SoundSource.PLAYERS, 0.9F, hitAnything ? 1.1F : 0.8F);

        if (hitAnything) {
            // Only a connecting dive rebounds. Rebounding off an empty floor would make the dive
            // strictly better than a jump, and a move with no cost is a move with no decision.
            player.setDeltaMovement(player.getDeltaMovement().x, DIVE_REBOUND,
                    player.getDeltaMovement().z);
            player.hurtMarked = true;
            player.resetFallDistance();
        }
    }

    private static void trail(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level && player.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 0.8D, player.getZ(), 2, 0.15D, 0.2D, 0.15D, 0.01D);
        }
    }

    /**
     * The wall cling: hold against a wall to hang almost stationary.
     *
     * <p>Distinct from the wall slide in {@code AirMoveService}, which is off by default because it
     * granted a free jump. This grants no jump at all — it only buys time, and it runs out. A cling
     * that lasted forever would remove every vertical hazard in the game, so the timer is the
     * mechanic rather than a limitation of it.
     */
    private static void tickCling(ServerPlayer player) {
        if (player.onGround()) {
            CLING_TICKS.remove(player);
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        boolean touchingWall = player.horizontalCollision;
        if (!touchingWall || velocity.y > 0.0D) {
            CLING_TICKS.remove(player);
            return;
        }

        int held = CLING_TICKS.getOrDefault(player, 0);
        if (held >= CLING_MAX_TICKS) {
            // Out of grip: fall normally rather than snapping, so the failure is readable.
            return;
        }
        CLING_TICKS.put(player, held + 1);

        if (velocity.y < CLING_FALL_SPEED) {
            player.setDeltaMovement(velocity.x, CLING_FALL_SPEED, velocity.z);
            player.hurtMarked = true;
            player.resetFallDistance();
        }
        if (player.level() instanceof ServerLevel level && held % 6 == 0) {
            level.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.2D, 0.3D, 0.2D, 0.0D);
        }
    }

    /** Clears per-player state on logout, course exit or losing the suit. */
    public static void forget(ServerPlayer player) {
        DIVING.remove(player);
        CLING_TICKS.remove(player);
    }

    /** Whether the player is mid-dive, for the HUD or a renderer that wants a pose. */
    public static boolean isDiving(ServerPlayer player) {
        return Boolean.TRUE.equals(DIVING.get(player));
    }

    /** Remaining cling grip in ticks, 0 when not clinging. */
    public static int clingRemaining(ServerPlayer player) {
        return Math.max(0, CLING_MAX_TICKS - CLING_TICKS.getOrDefault(player, CLING_MAX_TICKS));
    }

    /** Guard so the service is never used outside a course. */
    static boolean active(CourseState state) {
        return state.inCourse();
    }
}
