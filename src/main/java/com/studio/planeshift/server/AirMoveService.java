package com.studio.planeshift.server;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Air movement extras: hold jump to jump higher, crouch in air to ground-pound,
 * and hit question blocks from below by jumping into them.
 */
public final class AirMoveService {

    /** Small extra upward boost each tick while the player is still rising. */
    private static final double HOLD_JUMP_BOOST = 0.04D;
    /** Downward speed during a ground pound. */
    private static final double GROUND_POUND_SPEED = -1.2D;
    /** Ticks a given block ignores repeat head-bumps from the same player. */
    private static final int HIT_DEBOUNCE_TICKS = 10;

    /** Downward kick after a head bump, so the hit reads as a collision not a swallowed jump. */
    private static final double HEAD_BUMP_REBOUND = -0.12D;
    /** Capped descent while clinging to a wall. */
    private static final double WALL_SLIDE_SPEED = -0.12D;
    /** Launch velocity from a wall jump. */
    private static final double WALL_JUMP_STRENGTH = 0.52D;
    /** How long after leaving a wall a jump still counts as a wall jump. */
    private static final int WALL_JUMP_GRACE_TICKS = 6;

    /** Game time of the last wall contact per player, for the wall-jump window. */
    private static final Map<ServerPlayer, Long> WALL_CONTACT = new WeakHashMap<>();

    /** Weak keys so a disconnected player cannot keep an entry alive. */
    private static final Map<ServerPlayer, LastHit> LAST_HIT = new WeakHashMap<>();

    /** Tracks if the player is currently executing a ground pound. */
    private static final Map<ServerPlayer, Boolean> GROUND_POUNDING = new WeakHashMap<>();

    private AirMoveService() {
    }

    public static void tick(ServerPlayer player) {
        if (!CourseStateAccess.get(player).inCourse()) {
            return;
        }

                // Variable jump: if the player is still moving upward, keep giving a tiny boost.
        // Holding space = longer air time = higher jump in practice.
        if (!player.onGround() && player.getLastClientInput().jump() && player.getDeltaMovement().y > 0.0D && player.getDeltaMovement().y < 0.45D) {
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(vel.x, vel.y + HOLD_JUMP_BOOST, vel.z);
            player.hurtMarked = true;
        }

        // Ground pound mechanics
        if (!player.onGround() && player.isShiftKeyDown() && player.getDeltaMovement().y <= 0.0D) {
            player.setDeltaMovement(new Vec3(0.0D, GROUND_POUND_SPEED, 0.0D));
            player.hurtMarked = true;
            player.resetFallDistance();
            GROUND_POUNDING.put(player, true);
        } else if (player.onGround()) {
            if (GROUND_POUNDING.remove(player) != null) {
                // Just landed from a ground pound. Break bricks below!
                BlockPos pos = player.blockPosition();
                BlockPos below = pos.below();
                BlockState state = player.level().getBlockState(below);
                if (state.getBlock() instanceof com.studio.planeshift.common.block.BrickBlock || 
                    state.getBlock() instanceof com.studio.planeshift.common.block.QuestionBlock) {
                    // Task 56: Brick debris particles
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                net.minecraft.core.particles.ParticleTypes.CRIT,
                                below.getX() + 0.5D, below.getY() + 0.5D, below.getZ() + 0.5D,
                                20, 0.3D, 0.3D, 0.3D, 0.1D);
                    }
                    player.level().destroyBlock(below, true);
                    player.level().playSound(null, below, com.studio.planeshift.common.registry.ModSounds.BRICK_BREAK.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }

        // Head-hit question blocks from below.
        if (player.getDeltaMovement().y > 0.0D) {
            BlockPos headPos = BlockPos.containing(player.getX(), player.getEyeY() + 0.1D, player.getZ()).above();
            BlockState state = player.level().getBlockState(headPos);
            Block block = state.getBlock();
            if (block instanceof com.studio.planeshift.common.block.HitFromBelowBlock hit
                    && consumeHit(player, headPos)) {
                hit.attemptHitFromBelow(state, player.level(), headPos, player);
                // Rebound off the block rather than stopping dead against it. Vanilla would
                // simply zero the upward velocity, which reads as the jump being swallowed;
                // a short downward kick makes the hit feel like a collision.
                Vec3 velocity = player.getDeltaMovement();
                player.setDeltaMovement(velocity.x, HEAD_BUMP_REBOUND, velocity.z);
                player.hurtMarked = true;
            }
        }

        tickWallSlide(player);
    }

    /**
     * Wall slide and wall jump.
     *
     * <p>While the player is airborne, falling and pressed against a wall, the descent is capped
     * so they visibly cling rather than drop. That slide also refreshes the wall-jump window, so
     * a jump pressed during it launches them upward again.
     *
     * <p>The window is what makes this a mechanic rather than infinite climbing: it expires
     * shortly after the player leaves the wall, and it only refreshes on contact, so each push
     * off costs a fresh wall.
     */
    private static void tickWallSlide(ServerPlayer player) {
        if (player.onGround()) {
            WALL_CONTACT.remove(player);
        GROUND_POUNDING.remove(player);
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        boolean touchingWall = player.horizontalCollision;

        if (touchingWall && velocity.y < 0.0D) {
            // Cling: cap the fall so the slide is readable and gives time to react.
            if (velocity.y < WALL_SLIDE_SPEED) {
                player.setDeltaMovement(velocity.x, WALL_SLIDE_SPEED, velocity.z);
                player.hurtMarked = true;
                player.resetFallDistance();
            }
            WALL_CONTACT.put(player, player.level().getGameTime());
            return;
        }

        Long contact = WALL_CONTACT.get(player);
        if (contact == null) {
            return;
        }
        long since = player.level().getGameTime() - contact;
        if (since > WALL_JUMP_GRACE_TICKS) {
            WALL_CONTACT.remove(player);
        GROUND_POUNDING.remove(player);
            return;
        }
        // A jump pressed inside the grace window converts the slide into a fresh launch. The
        // player has already left the wall by this tick, which is why the window exists at all.
        if (player.getLastClientInput().jump() && velocity.y <= 0.0D) {
            player.setDeltaMovement(velocity.x, WALL_JUMP_STRENGTH, velocity.z);
            player.hurtMarked = true;
            player.resetFallDistance();
            WALL_CONTACT.remove(player);
        GROUND_POUNDING.remove(player);
        }
    }

    /**
     * One bump per jump per block. A rising player stays under the same block for several
     * ticks, so without this a single jump would drain every coin from a coin block and
     * flip an ON/OFF switch back and forth repeatedly.
     */
    private static boolean consumeHit(ServerPlayer player, BlockPos pos) {
        long now = player.level().getGameTime();
        long packed = pos.asLong();
        LastHit last = LAST_HIT.get(player);
        if (last != null && last.pos == packed && now - last.tick < HIT_DEBOUNCE_TICKS) {
            return false;
        }
        LAST_HIT.put(player, new LastHit(packed, now));
        return true;
    }

    /** Cleans up the debounce entry when a player disconnects. */
    public static void forget(ServerPlayer player) {
        LAST_HIT.remove(player);
        WALL_CONTACT.remove(player);
        GROUND_POUNDING.remove(player);
    }

    private record LastHit(long pos, long tick) {
    }
}


