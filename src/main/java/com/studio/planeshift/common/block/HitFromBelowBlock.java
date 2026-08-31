package com.studio.planeshift.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks that react when the player jumps into them from below.
 */
public interface HitFromBelowBlock {

    /** Called by {@link com.studio.planeshift.server.AirMoveService} when the player's head touches this block. */
    void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player);
}
