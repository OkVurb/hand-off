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

    /** Weak keys so a disconnected player cannot keep an entry alive. */
    private static final Map<ServerPlayer, LastHit> LAST_HIT = new WeakHashMap<>();

    private AirMoveService() {
    }

    public static void tick(ServerPlayer player) {
        if (!CourseStateAccess.get(player).inCourse()) {
            return;
        }

        // Variable jump: if the player is still moving upward, keep giving a tiny boost.
        // Holding space = longer air time = higher jump in practice.
        if (!player.onGround() && player.getDeltaMovement().y > 0.0D && player.getDeltaMovement().y < 0.35D) {
            player.move(MoverType.SELF, new Vec3(0.0D, HOLD_JUMP_BOOST, 0.0D));
            player.hurtMarked = true;
        }

        // Ground pound: crouch in mid-air to slam downward.
        if (!player.onGround() && player.isShiftKeyDown() && player.getDeltaMovement().y > GROUND_POUND_SPEED) {
            player.setDeltaMovement(new Vec3(0.0D, GROUND_POUND_SPEED, 0.0D));
            player.hurtMarked = true;
            player.resetFallDistance();
        }

        // Head-hit question blocks from below.
        if (player.getDeltaMovement().y > 0.0D) {
            BlockPos headPos = BlockPos.containing(player.getX(), player.getEyeY() + 0.1D, player.getZ()).above();
            BlockState state = player.level().getBlockState(headPos);
            Block block = state.getBlock();
            if (block instanceof com.studio.planeshift.common.block.HitFromBelowBlock hit
                    && consumeHit(player, headPos)) {
                hit.attemptHitFromBelow(state, player.level(), headPos, player);
            }
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
    }

    private record LastHit(long pos, long tick) {
    }
}
