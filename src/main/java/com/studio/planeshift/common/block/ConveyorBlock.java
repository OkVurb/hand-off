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
    public static final double SPEED = 0.06D;
    protected static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public ConveyorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
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
        net.minecraft.world.phys.Vec3 current = entity.getDeltaMovement();
        
        double targetX = current.x;
        double targetZ = current.z;
        
        if (Math.abs(current.x) < 0.2D) targetX += facing.getStepX() * SPEED;
        if (Math.abs(current.z) < 0.2D) targetZ += facing.getStepZ() * SPEED;
        
        entity.setDeltaMovement(targetX, current.y, targetZ);
        entity.hurtMarked = true;
    }
}
