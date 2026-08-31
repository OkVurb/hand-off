package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A breakable brick in the Mario style: hit it from below to destroy it.
 *
 * <p>No drops by design — the fun is clearing the path, not grinding resources.
 */
public class BrickBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<BrickBlock> CODEC = simpleCodec(BrickBlock::new);

    public BrickBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return;
        }
        if (HitFromBelowBlock.isHeadContact(player, pos)) {
            level.destroyBlock(pos, false);
            level.playSound(null, pos, ModSounds.BRICK_BREAK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        attack(state, level, pos, player);
    }
}
