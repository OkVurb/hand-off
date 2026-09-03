package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
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
 * Super Mario World-style Rotating Block (Turn Block).
 * When hit from below, it spins and becomes passable for 4 seconds, then solidifies.
 */
public class RotatingBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<RotatingBlock> CODEC = simpleCodec(RotatingBlock::new);
    public static final BooleanProperty SPINNING = BooleanProperty.create("spinning");

    /** Ticks the block spends spinning before becoming solid again (80 ticks = 4 seconds). */
    public static final int SPIN_DURATION_TICKS = 80;

    public RotatingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SPINNING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPINNING);
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || state.getValue(SPINNING)) {
            return;
        }
        if (!HitFromBelowBlock.isHeadContact(player, pos)) {
            return;
        }
        triggerSpin(state, level, pos);
    }

    public void triggerSpin(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(SPINNING)) {
            return;
        }
        level.setBlock(pos, state.setValue(SPINNING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, SPIN_DURATION_TICKS);
        level.playSound(null, pos, ModSounds.QUESTION_BUMP.get(), SoundSource.BLOCKS, 0.9F, 1.4F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SPINNING)) {
            level.setBlock(pos, state.setValue(SPINNING, false), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.QUESTION_BUMP.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SPINNING) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SPINNING) ? Shapes.empty() : super.getShape(state, level, pos, context);
    }
}
