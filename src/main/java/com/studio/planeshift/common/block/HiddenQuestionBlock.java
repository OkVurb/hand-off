package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A question block that is invisible until hit from below.
 *
 * <p>Once a player jumps into it, the block reveals itself as an empty question block
 * and pops a pickup exactly like a normal ? block. Before the hit it has no visible
 * faces, so it blends into the background as a secret.
 */
public class HiddenQuestionBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<HiddenQuestionBlock> CODEC = simpleCodec(HiddenQuestionBlock::new);

    public HiddenQuestionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        if (player.getY() < pos.getY()) {
            BlockState revealed = com.studio.planeshift.common.registry.ModBlocks.QUESTION_BLOCK.get()
                    .defaultBlockState()
                    .setValue(QuestionBlock.USED, true);
            level.setBlock(pos, revealed, Block.UPDATE_ALL);
            QuestionBlock.popPickup(level, pos);
            level.playSound(null, pos, com.studio.planeshift.common.registry.ModSounds.QUESTION_BUMP.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        attack(state, level, pos, player);
    }
}
