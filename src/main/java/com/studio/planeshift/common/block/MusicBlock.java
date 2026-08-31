package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Music block: plays a course track when stepped on or hit from below.
 */
public class MusicBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<MusicBlock> CODEC = simpleCodec(MusicBlock::new);

    public MusicBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer
                && level.getGameTime() % 40L == 0L) {
            play(level, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        play(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        play(level, pos);
    }

    private void play(Level level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.MUSIC_COURSE_2_5D.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
