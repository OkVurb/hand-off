package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.server.CheckpointService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Relay beacon checkpoint (Design Bible, "Blocks, objects, hazards, and portals").
 *
 * <p>Contract: "Party policy, ledger snapshot, visible activation." Touching the beacon
 * stores a per-player checkpoint; the LIT state is world-visible feedback. Activation is
 * server-side and idempotent.
 */
public class CheckpointBeaconBlock extends Block {

    public static final MapCodec<CheckpointBeaconBlock> CODEC = simpleCodec(CheckpointBeaconBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE = Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);

    public CheckpointBeaconBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean isPrimary) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            boolean newlyActivated = CheckpointService.activate(player, pos);
            if (newlyActivated && !state.getValue(LIT)) {
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            }
        }
    }
}
