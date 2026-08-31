package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Note block: bounces the player upward and plays a sound when jumped on.
 * Hitting from below also plays a note.
 */
public class PlaneshiftNoteBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<PlaneshiftNoteBlock> CODEC = simpleCodec(PlaneshiftNoteBlock::new);
    public static final double BOUNCE = 1.0D;

    public PlaneshiftNoteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity.getDeltaMovement().y < 0.0D) {
            entity.setDeltaMovement(entity.getDeltaMovement().x, BOUNCE, entity.getDeltaMovement().z);
            entity.hurtMarked = true;
            entity.resetFallDistance();
            playNote(level, pos);
        }
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        playNote(level, pos);
    }

    private void playNote(Level level, BlockPos pos) {
        float pitch = 0.9F + Mth.sin(pos.getX() * 17 + pos.getY() * 31 + pos.getZ() * 7) * 0.3F;
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.BLOCKS, 0.8F, pitch);
    }
}
