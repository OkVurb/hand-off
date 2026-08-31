package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * ON/OFF block: solid and lit when ON, non-solid and dark when OFF.
 */
public class OnOffBlock extends Block {

    public static final MapCodec<OnOffBlock> CODEC = simpleCodec(OnOffBlock::new);
    public static final BooleanProperty ON = BooleanProperty.create("on");

    public OnOffBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ON, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ON);
    }

    public void setState(Level level, BlockPos pos, boolean on) {
        BlockState state = level.getBlockState(pos);
        if (state.getValue(ON) == on) {
            return;
        }
        level.setBlock(pos, state.setValue(ON, on), Block.UPDATE_ALL);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ON) ? super.getCollisionShape(state, level, pos, context) : Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }
}
