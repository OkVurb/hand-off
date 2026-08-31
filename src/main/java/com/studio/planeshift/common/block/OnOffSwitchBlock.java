package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * ON/OFF switch: hit to toggle all ON/OFF blocks in the course.
 */
public class OnOffSwitchBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<OnOffSwitchBlock> CODEC = simpleCodec(OnOffSwitchBlock::new);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    /**
     * Search box for linked ON/OFF blocks. Kept deliberately flat: a symmetric ±48 cube
     * covers ~912k blocks, and even a palette-skipping scan has to walk every section it
     * cannot reject. See {@link BlockAreaScan} for how the box is searched.
     */
    private static final int RANGE_XZ = 24;
    private static final int RANGE_Y = 8;

    public OnOffSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, false);
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        toggle(level, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        toggle(level, pos, state);
        return InteractionResult.SUCCESS;
    }

    private void toggle(Level level, BlockPos pos, BlockState state) {
        boolean next = !state.getValue(POWERED);
        level.setBlock(pos, state.setValue(POWERED, next), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, next ? 1.2F : 0.8F);

        // Only the blocks that actually need flipping, so the palette filter can reject whole
        // sections that hold no ON/OFF block in the wrong state.
        List<BlockPos> targets = BlockAreaScan.findMatching(level, pos, RANGE_XZ, RANGE_Y,
                target -> target.getBlock() instanceof OnOffBlock && target.getValue(OnOffBlock.ON) != next);
        for (BlockPos target : targets) {
            BlockState targetState = level.getBlockState(target);
            if (targetState.getBlock() instanceof OnOffBlock onOff) {
                onOff.setState(level, target, next);
            }
        }
    }
}
