package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.server.ModeTransitionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * A course-authorized perspective gate (Design Bible, "Mode transition transaction").
 *
 * <p>"Trigger: tagged gate/volume by default; manual switch only where course metadata
 * allows it." Touching the gate volume asks the server to open a shift transaction
 * toward {@link #TARGET}; the server performs every validation. The gate's facing
 * defines the destination rail when shifting into 2.5D, so gates work in any world
 * without a course JSON.
 */
public class ShiftGateBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<ShiftGateBlock> CODEC = simpleCodec(ShiftGateBlock::new);
    public static final EnumProperty<PlaneMode> TARGET = EnumProperty.create("target", PlaneMode.class);

    /** Thin upright pane so walking through reads as crossing a seam, not hitting a wall. */
    private static final VoxelShape SHAPE_NS = Shapes.box(0.0D, 0.0D, 7.0D / 16.0D, 1.0D, 1.0D, 9.0D / 16.0D);
    private static final VoxelShape SHAPE_EW = Shapes.box(7.0D / 16.0D, 0.0D, 0.0D, 9.0D / 16.0D, 1.0D, 1.0D);

    public ShiftGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TARGET, PlaneMode.SIDE_ON));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TARGET);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the placer, and default to the opposite of nothing: placer chooses target
        // by sneaking (sneak-place = gate back to free 3D).
        PlaneMode target = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? PlaneMode.FREE_3D
                : PlaneMode.SIDE_ON;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(TARGET, target);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST, WEST -> SHAPE_EW;
            default -> SHAPE_NS;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean isPrimary) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            ModeTransitionService.requestGateShift(player, pos, state.getValue(TARGET), state.getValue(FACING));
        }
    }
}
