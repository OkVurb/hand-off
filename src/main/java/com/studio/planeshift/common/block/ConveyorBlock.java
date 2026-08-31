package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Conveyor belt: pushes any entity standing on it in the direction it faces.
 */
public class ConveyorBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);
    public static final double SPEED = 0.18D;

    public ConveyorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide()) {
            return;
        }
        Direction facing = state.getValue(FACING);
        entity.setDeltaMovement(
                facing.getStepX() * SPEED,
                entity.getDeltaMovement().y,
                facing.getStepZ() * SPEED
        );
        entity.hurtMarked = true;
    }
}
