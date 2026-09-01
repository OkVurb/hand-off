package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LoopTriggerBlock extends Block {

    public static final MapCodec<LoopTriggerBlock> CODEC = simpleCodec(LoopTriggerBlock::new);
    public static final int LOOP_DISTANCE = 25;

    public LoopTriggerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean wasInside) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        // Seamlessly teleport the player backward to loop the corridor
        player.teleportTo(player.getX() - LOOP_DISTANCE, player.getY(), player.getZ());
    }
}
