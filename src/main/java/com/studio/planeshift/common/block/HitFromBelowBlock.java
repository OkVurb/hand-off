package com.studio.planeshift.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blocks that react when the player jumps into them from below.
 */
public interface HitFromBelowBlock {

    /**
     * How far the player's head may be beneath the block's underside and still count as
     * contact. One block: enough to cover a crouched pose, whose head sits lower than the
     * block {@code AirMoveService} anticipates, but far short of the ~4.5 block interaction
     * reach that made the old whole-player check fire from anywhere below.
     */
    double MAX_HEAD_GAP = 1.0D;

    /**
     * How far the head may have already crossed into the block. Head contact is detected on
     * the tick the player is still rising, so a little penetration is normal.
     */
    double HEAD_OVERLAP = 0.25D;

    /** Horizontal slack on the block column, so a bump at the very edge still registers. */
    double COLUMN_SLACK = 0.2D;

    /** Called by {@link com.studio.planeshift.server.AirMoveService} when the player's head touches this block. */
    void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player);

    /**
     * Triggers whatever the block at {@code pos} does when something hits it that is <em>not</em>
     * a head bump: a ground pound landing on top of it, or a kicked Koopa shell running into it.
     *
     * <p>Exists because {@link #attemptHitFromBelow} cannot serve those callers. It takes a
     * {@link Player} — a shell has none — and every implementation gates on
     * {@link #isHeadContact}, which a ground pound fails by definition, since the whole point is
     * that the player is above the block rather than beneath it.
     *
     * <p>Before this existed the ground pound simply called {@code level.destroyBlock(pos, true)}
     * on anything it landed on. That is wrong twice over: a question block dropped <em>itself</em>
     * as an item instead of paying out its power-up, and a coin block was deleted along with
     * every coin still inside it. A block's reward should not depend on which side you hit it
     * from.
     *
     * @return true if the block reacted, so the caller can decide whether to play its own effects
     */
    static boolean impact(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof QuestionBlock question) {
            question.triggerFromImpact(state, level, pos);
            return true;
        }
        if (state.getBlock() instanceof CoinBlock) {
            CoinBlock.payOne(state, level, pos);
            return true;
        }
        if (state.getBlock() instanceof RotatingBlock rotating) {
            rotating.triggerSpin(state, level, pos);
            return true;
        }
        if (state.getBlock() instanceof BrickBlock) {
            return BrickBlock.impact(state, level, pos);
        }
        return false;
    }

    /**
     * Whether {@code player} is underneath {@code pos} and close enough for the hit to read as
     * a head bump.
     *
     * <p>Replaces {@code player.getY() < pos.getY()}, which only asked whether the player was
     * *somewhere* below. Left-clicking reaches about 4.5 blocks, so that test let a player
     * standing well underneath — or off to the side, looking up at the block's edge — trigger
     * a block they never touched. This uses the player's bounding box instead: the top of it
     * has to sit at the block's underside, and the box has to overlap the block's own column.
     *
     * <p>Deliberately tolerant rather than exact. The two callers disagree slightly about
     * timing — {@code AirMoveService} fires on the block the rising player is *about* to enter,
     * while a left-click fires while they are standing still — and a crouched player's head is
     * lower again. {@link #MAX_HEAD_GAP} is sized to cover all three.
     */
    static boolean isHeadContact(Player player, BlockPos pos) {
        AABB box = player.getBoundingBox();
        double blockBottom = pos.getY();

        // Approached from underneath: the head has not risen past the block's lower face by
        // more than the overlap a rising player accumulates within one tick.
        if (box.maxY > blockBottom + HEAD_OVERLAP) {
            return false;
        }
        // ...and is actually at the block rather than metres beneath it.
        if (blockBottom - box.maxY > MAX_HEAD_GAP) {
            return false;
        }
        // ...and beneath the block itself, not a neighbouring column.
        return box.maxX > pos.getX() - COLUMN_SLACK
                && box.minX < pos.getX() + 1.0D + COLUMN_SLACK
                && box.maxZ > pos.getZ() - COLUMN_SLACK
                && box.minZ < pos.getZ() + 1.0D + COLUMN_SLACK;
    }
}
