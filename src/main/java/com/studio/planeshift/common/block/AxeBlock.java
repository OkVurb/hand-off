package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The axe at the end of a castle: taking it drops the bridge out from under the boss.
 *
 * <p>Collapse runs one tile per scheduled tick, walking back from the axe toward the far side.
 * That sequencing is the whole spectacle — removing the bridge in a single frame would read as a
 * bug, while a visible cascade reads as a consequence of the player's action.
 *
 * <p>The scan walks a bounded run of {@link #MAX_BRIDGE_LENGTH} and stops at the first tile that
 * is not bridge, so the collapse cannot escape into arbitrary terrain.
 */
public class AxeBlock extends Block {

    public static final MapCodec<AxeBlock> CODEC = simpleCodec(AxeBlock::new);

    /** True once the axe has been taken; a spent axe does nothing. */
    public static final BooleanProperty TAKEN = BooleanProperty.create("taken");
    /** Tiles collapsed so far, so the sequence survives a save mid-collapse. */
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, 15);

    /** Longest bridge the collapse will walk. Bounds the work regardless of world contents. */
    private static final int MAX_BRIDGE_LENGTH = 15;
    /** Ticks between tiles falling. Fast enough to feel urgent, slow enough to watch. */
    private static final int COLLAPSE_INTERVAL = 3;
    /** Direction the bridge runs from the axe. Courses build west-to-east along +X. */
    private static final Direction BRIDGE_DIRECTION = Direction.WEST;

    public AxeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TAKEN, false).setValue(STEP, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TAKEN, STEP);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(TAKEN)) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, state.setValue(TAKEN, true).setValue(STEP, 0), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, COLLAPSE_INTERVAL);
        level.playSound(null, pos, ModSounds.COURSE_CLEAR.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Also triggers on a head bump, so the axe can be taken from below like everything else. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        useWithoutItem(state, level, pos, player, null);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(TAKEN)) {
            return;
        }
        int step = state.getValue(STEP);
        if (step >= MAX_BRIDGE_LENGTH) {
            return;
        }

        BlockPos target = pos.relative(BRIDGE_DIRECTION, step + 1);
        BlockState targetState = level.getBlockState(target);
        if (!isBridge(targetState)) {
            // Reached the far end of the bridge; nothing further to drop.
            return;
        }

        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, target, ModSounds.BRICK_BREAK.get(), SoundSource.BLOCKS, 0.8F, 0.7F);
        level.setBlock(pos, state.setValue(STEP, step + 1), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, COLLAPSE_INTERVAL);
    }

    /**
     * What counts as bridge. Restricted to the castle palette so a collapse cannot chew through
     * whatever a player happened to build next to the arena.
     */
    private static boolean isBridge(BlockState state) {
        return state.is(ModBlocks.COURSE_CASTLE_BLOCK.get())
                || state.is(ModBlocks.BRICK_BLOCK.get());
    }
}
