package com.studio.planeshift.server;

import com.studio.planeshift.common.PlaneShiftConfig;
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

    /**
     * How many ticks of variable-jump boost one press is worth.
     *
     * <p>This is the bug fix. The boost had no budget at all: it applied on every tick the player
     * was rising slower than 0.45, and the boost it added fed straight back into the rise it was
     * gated on. Because 0.04 per tick is a shade under what gravity takes away, the rise settled
     * into an equilibrium *below* the 0.45 ceiling and simply never exceeded it, so holding jump
     * climbed forever. A double jump made it trivial to reach, because it re-entered the rising
     * state in mid-air with a fresh upward velocity.
     *
     * <p>Ten ticks is half a second, which is roughly the window a 2D Mario game gives you to
     * decide how high a jump is — long enough that holding jump is visibly a taller jump, far
     * short of flight.
     */
    private static final int MAX_BOOST_TICKS = 10;

    /** Remaining boost ticks. Refilled on the ground and on a fresh press, never by holding. */
    private static final Map<ServerPlayer, Integer> BOOST_BUDGET = new WeakHashMap<>();

    /** Whether jump was held last tick, so a press can be told apart from a hold. */
    private static final Map<ServerPlayer, Boolean> JUMP_HELD = new WeakHashMap<>();
    /** Downward speed during a ground pound. */
    private static final double GROUND_POUND_SPEED = -1.2D;
    /** Ticks a given block ignores repeat head-bumps from the same player. */
    private static final int HIT_DEBOUNCE_TICKS = 10;

    /**
     * How far past the top of the head to look for the block being hit.
     *
     * <p>Large enough to cross into the block that stopped the player's rise, small enough that
     * it cannot reach the block above that one.
     */
    private static final double HEAD_PROBE = 0.1D;

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

    /** Previous tick's Y, so upward movement can be measured rather than read from velocity. */
    private static final Map<ServerPlayer, Double> LAST_Y = new WeakHashMap<>();

    /** Ticks a player still counts as rising after their climb has been stopped. */
    private static final Map<ServerPlayer, Integer> RISING_GRACE = new WeakHashMap<>();

    /**
     * How long a bump stays eligible after upward movement stops.
     *
     * <p>Three ticks. The block that ends a jump also ends the rise, so without a window the
     * contact tick is never eligible; long enough to survive a dropped movement packet, short
     * enough that a falling player cannot trigger a block above them.
     */
    private static final int HEAD_BUMP_GRACE_TICKS = 3;

    /** Tracks if the player is currently executing a ground pound. */
    private static final Map<ServerPlayer, Boolean> GROUND_POUNDING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Boolean> SPIN_JUMPING = new WeakHashMap<>();

    /**
     * Ticks left before the mantle may fire again.
     *
     * <p>Without this the clamber is a wall climb. The assist sets upward velocity to 0.28, which
     * decays back under the 0.1 trigger threshold within a few ticks; if the player is still
     * pressed into the same ledge the condition is true again and they get another 0.28. Repeat
     * and any one-block lip becomes an elevator, which quietly invalidates every height the
     * reachability solver reasons about. One assist per airborne stint is the whole feature.
     */
    private static final Map<ServerPlayer, Integer> CLAMBER_COOLDOWN = new WeakHashMap<>();

    /** Last measured horizontal position, for a velocity the server can actually trust. */
    private static final Map<ServerPlayer, double[]> LAST_XZ = new WeakHashMap<>();

    private static final int CLAMBER_COOLDOWN_TICKS = 12;

    /**
     * Ticks of stall left, and whether this airborne stint has already spent its one stall.
     *
     * <p>The stall is the forgiveness half of the spin: a brief hover that lets a jump reach a
     * platform it would otherwise fall just short of. Once per airborne stint, recharged only by
     * landing, so it extends a jump rather than replacing gravity.
     */
    private static final Map<ServerPlayer, Integer> STALL_TICKS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Boolean> STALL_SPENT = new WeakHashMap<>();

    /** How long the hover lasts. Short: it buys a block of reach, not a flight. */
    private static final int STALL_DURATION = 8;

    /** Downward speed held during a stall. Not zero — a dead stop reads as a bug, not a hover. */
    private static final double STALL_FALL_SPEED = -0.045D;

    /**
     * Whether this player is mid-ground-pound.
     *
     * <p>Read by {@code CourseEnemyEntity.playerTouch} so a pound that lands on an enemy resolves
     * as a pound rather than as an ordinary stomp. Without it the armoured branch runs, and the
     * move designed to beat armour would instead hurt the player using it.
     */
    public static boolean isGroundPounding(net.minecraft.world.entity.player.Player player) {
        return GROUND_POUNDING.getOrDefault(player, false);
    }

    public static boolean isSpinJumping(net.minecraft.world.entity.player.Player player) {
        return SPIN_JUMPING.getOrDefault(player, false);
    }

    private AirMoveService() {
    }

    public static void tick(ServerPlayer player) {
        if (!CourseStateAccess.get(player).inCourse()) {
            return;
        }

        // Spin jump: jump while sneaking, from the ground.
        //
        // Edge-triggered on purpose. Written as a plain per-tick condition it re-fires — and
        // re-plays its sound — every tick the player stands there holding both keys, which is
        // most audible in exactly the case where the jump is blocked by a low ceiling.
        boolean spinning = SPIN_JUMPING.getOrDefault(player, false);
        if (player.onGround() && player.isShiftKeyDown() && player.getLastClientInput().jump()) {
            if (!spinning) {
                SPIN_JUMPING.put(player, true);
                spinning = true;
                player.level().playSound(null, player.blockPosition(),
                        com.studio.planeshift.common.registry.ModSounds.POWER_UP.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.6F);
                // The spin is an attack as well as a jump. Swept here, on the ground, on the edge
                // — so it fires once per spin and hits what was beside you when you started it.
                if (player.level() instanceof net.minecraft.server.level.ServerLevel spinLevel) {
                    SpinAttackService.strike(spinLevel, player);
                }
            }
        } else if (player.onGround()) {
            SPIN_JUMPING.remove(player);
            spinning = false;
        }

        if (spinning && !player.onGround()) {
            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl && player.level().getGameTime() % 2 == 0) {
                sl.sendParticles(com.studio.planeshift.common.registry.ModParticles.COIN_SPARKLE.get(), player.getX(), player.getY() + 0.5D, player.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.05D);
            }
        }

        // Measured per-tick movement.
        //
        // Hoisted above every velocity write below, because all of them need it. The client owns
        // player movement and the server applies it from position packets, so a ServerPlayer's
        // getDeltaMovement() is routinely stale — most visibly in its horizontal components,
        // which the server rebuilds from lagging input flags. The difference between two
        // consecutive positions is the one thing the server definitely knows.
        double[] lastXz = LAST_XZ.get(player);
        double moveX = lastXz == null ? 0.0D : player.getX() - lastXz[0];
        double moveZ = lastXz == null ? 0.0D : player.getZ() - lastXz[1];
        LAST_XZ.put(player, new double[] {player.getX(), player.getZ()});
        double measuredRise = player.getY() - LAST_Y.getOrDefault(player, player.getY());

        // Variable jump: while the player is still rising and still holding jump, top up the
        // climb. Holding space = longer air time = higher jump in practice.
        //
        // The boost has to be *added* to what the player actually has, not to what the server
        // thinks they have. hurtMarked sends a velocity packet the client applies as an absolute
        // override, so writing getDeltaMovement() straight back out replaced the client's real
        // velocity with the server's stale copy on every tick of the rise. Two things broke as a
        // result: a player moving left or right lost their horizontal speed each tick — the jump
        // "didn't work well while moving" — and a client-side double jump from a movement mod was
        // flattened the tick after it fired, because the server had never seen the extra height
        // and cheerfully overwrote it with its own smaller number.
        //
        // Building the vector out of the measured deltas, and never sending a rise smaller than
        // the one just measured, makes the packet agree with what the client already did instead
        // of arguing with it.
        //
        // The budget is what stops this being flight. Standing on the ground refills it, and so
        // does a *fresh* press in mid-air — which is exactly what a double jump is, so a double
        // jump still gets its own variable height. Holding the key refills nothing, so the boost
        // can only ever extend a jump by MAX_BOOST_TICKS before gravity gets it back.
        boolean jumpDown = player.getLastClientInput().jump();
        boolean freshPress = jumpDown && !JUMP_HELD.getOrDefault(player, false);
        JUMP_HELD.put(player, jumpDown);

        int budget = BOOST_BUDGET.getOrDefault(player, MAX_BOOST_TICKS);
        if (player.onGround() || freshPress) {
            budget = MAX_BOOST_TICKS;
        }

        if (!player.onGround() && jumpDown && budget > 0
                && measuredRise > 0.0D && measuredRise < 0.45D) {
            double rise = Math.max(player.getDeltaMovement().y, measuredRise) + HOLD_JUMP_BOOST;
            player.setDeltaMovement(moveX, rise, moveZ);
            player.hurtMarked = true;
            budget--;
        }
        BOOST_BUDGET.put(player, budget);

        // Spin stall.
        //
        // Built from the measured horizontal deltas for the same reason the variable jump is:
        // hurtMarked sends an absolute velocity packet, so writing the server's stale x/z back out
        // every tick of an eight-tick hover would strip the player's horizontal speed exactly when
        // they are trying to cross a gap — which is the only situation a stall is ever used in.
        int stall = STALL_TICKS.getOrDefault(player, 0);
        if (player.onGround()) {
            STALL_SPENT.remove(player);
            stall = 0;
        } else if (spinning && stall == 0 && !STALL_SPENT.getOrDefault(player, false)
                && measuredRise < 0.0D) {
            // Armed on the way down, not on the way up: stalling at the apex is what turns a jump
            // into a glide, and one that fires before the player knows they are short is wasted.
            STALL_SPENT.put(player, true);
            stall = STALL_DURATION;
        }
        if (stall > 0) {
            player.setDeltaMovement(moveX, STALL_FALL_SPEED, moveZ);
            player.hurtMarked = true;
            player.resetFallDistance();
            stall--;
            if (player.level() instanceof net.minecraft.server.level.ServerLevel stallLevel) {
                stallLevel.sendParticles(com.studio.planeshift.common.registry.ModParticles.COIN_SPARKLE.get(),
                        player.getX(), player.getY() + 0.2D, player.getZ(), 1, 0.2D, 0.0D, 0.2D, 0.01D);
            }
        }
        STALL_TICKS.put(player, stall);

        // Ledge clamber / mantle. Direction from the same measured movement, for the same reason.

        int clamberCooldown = CLAMBER_COOLDOWN.getOrDefault(player, 0);
        if (player.onGround()) {
            clamberCooldown = 0;
        } else if (clamberCooldown > 0) {
            clamberCooldown--;
        }

        if (!player.onGround() && clamberCooldown == 0
                && player.getDeltaMovement().y < 0.1D && player.getDeltaMovement().y > -0.35D
                && Math.abs(moveX) + Math.abs(moveZ) > 0.02D) {
            BlockPos waistPos = BlockPos.containing(
                    player.getX() + Math.signum(moveX) * 0.45D,
                    player.getY() + 0.5D,
                    player.getZ() + Math.signum(moveZ) * 0.45D);
            BlockPos headPos = waistPos.above();
            if (player.level().getBlockState(waistPos).isSolid()
                    && player.level().getBlockState(headPos).isAir()) {
                player.setDeltaMovement(moveX * 1.1D, 0.28D, moveZ * 1.1D);
                player.hurtMarked = true;
                clamberCooldown = CLAMBER_COOLDOWN_TICKS;
            }
        }
        CLAMBER_COOLDOWN.put(player, clamberCooldown);

        // Ground pound mechanics.
        //
        // Explicitly excludes a spin jump. Both are bound to sneak, so without this guard the
        // spin jump converts into a ground pound the instant its rise ends — meaning the spin
        // jump's whole reason to exist, bouncing safely off a Spiny, could never happen.
        if (!player.onGround() && !spinning && player.isShiftKeyDown()
                && player.getDeltaMovement().y <= 0.0D) {
            player.setDeltaMovement(new Vec3(0.0D, GROUND_POUND_SPEED, 0.0D));
            player.hurtMarked = true;
            player.resetFallDistance();
            GROUND_POUNDING.put(player, true);
        } else if (player.onGround()) {
            if (GROUND_POUNDING.remove(player) != null) {
                // Landed from a ground pound: trigger whatever is underfoot.
                //
                // This used to call destroyBlock(below, true) on any brick, question or rotating
                // block. That deleted the block and dropped it as an *item*, so pounding a
                // question block handed the player a question block instead of the power-up
                // inside it, and pounding a coin block threw away every coin still in it. A
                // block's reward must not depend on which side it was hit from, so both this and
                // the Koopa shell now go through the same dispatcher.
                // The shockwave fires first and unconditionally: it is the half of the move
                // that works on open ground, where there is no block underfoot to trigger.
                if (player.level() instanceof net.minecraft.server.level.ServerLevel shockLevel) {
                    GroundPoundImpactService.shockwave(shockLevel, player.position(), player);
                }

                BlockPos below = player.blockPosition().below();
                if (com.studio.planeshift.common.block.HitFromBelowBlock.impact(player.level(), below)
                        && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.CRIT,
                            below.getX() + 0.5D, below.getY() + 1.0D, below.getZ() + 0.5D,
                            20, 0.3D, 0.1D, 0.3D, 0.1D);
                }
            }
        }

        // Head-hit question blocks from below.
        //
        // Gated on measured upward movement, not on getDeltaMovement().
        //
        // Two separate problems with reading velocity here. The client drives player movement and
        // the server applies it from position packets, so a ServerPlayer's delta is frequently
        // stale or zero even while the player is visibly rising. And on the exact tick the head
        // meets a block, the collision has already zeroed the upward velocity — so the one tick
        // that matters is the one tick the old condition rejected. Between them, most bumps were
        // simply never seen, which is why question blocks and coin blocks so often did nothing.
        //
        // Comparing this tick's Y against last tick's answers the real question — is this player
        // going up — from data the server definitely has. The grace window keeps the contact tick
        // eligible after the rise has already been stopped by the block itself.
        LAST_Y.put(player, player.getY());
        boolean rising = measuredRise > 0.001D;
        if (rising) {
            RISING_GRACE.put(player, HEAD_BUMP_GRACE_TICKS);
        } else {
            int grace = RISING_GRACE.getOrDefault(player, 0);
            if (grace > 0) {
                RISING_GRACE.put(player, grace - 1);
            }
        }
        boolean recentlyRising = rising || RISING_GRACE.getOrDefault(player, 0) > 0;

        if (recentlyRising && !player.onGround()) {
            // The block the top of the head is entering.
            //
            // This used to be eye height plus a tenth, then .above(). Eye height sits about 1.62
            // above the feet, so that expression lands on the correct block only while the eyes
            // are in the lower part of their block — the moment a rising player's eyes cross a
            // block boundary, .above() points one block too high and the bump silently misses.
            // Since the player is always rising when this runs, it missed constantly, which is
            // why question blocks and bricks so often did nothing.
            //
            // Measuring from the top of the bounding box instead targets the block the head is
            // actually about to touch, at any sub-block position and at any pose: collision stops
            // a rising player with maxY exactly at the block's underside, so maxY plus a small
            // epsilon lands inside the block that stopped them.
            BlockPos headPos = BlockPos.containing(
                    player.getX(), player.getBoundingBox().maxY + HEAD_PROBE, player.getZ());
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
        STALL_TICKS.remove(player);
        STALL_SPENT.remove(player);
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
        STALL_TICKS.remove(player);
        STALL_SPENT.remove(player);
            return;
        }
        // A jump pressed inside the grace window converts the slide into a fresh launch. The
        // player has already left the wall by this tick, which is why the window exists at all.
        //
        // Off by default. In a course packed with blocks almost any airborne moment is within six
        // ticks of touching something, so this fired constantly and read as a free double jump
        // rather than as a wall jump. Anyone who wants the mechanic can switch it back on.
        if (!PlaneShiftConfig.SERVER.wallJump.get()) {
            return;
        }
        if (player.getLastClientInput().jump() && velocity.y <= 0.0D) {
            player.setDeltaMovement(velocity.x, WALL_JUMP_STRENGTH, velocity.z);
            player.hurtMarked = true;
            player.resetFallDistance();
            WALL_CONTACT.remove(player);
        GROUND_POUNDING.remove(player);
        STALL_TICKS.remove(player);
        STALL_SPENT.remove(player);
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
        LAST_Y.remove(player);
        BOOST_BUDGET.remove(player);
        JUMP_HELD.remove(player);
        RISING_GRACE.remove(player);
        WALL_CONTACT.remove(player);
        GROUND_POUNDING.remove(player);
        STALL_TICKS.remove(player);
        STALL_SPENT.remove(player);
    }

    private record LastHit(long pos, long tick) {
    }
}


