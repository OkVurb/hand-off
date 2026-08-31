package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModBlocks;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.RandomSource;

/**
 * P-switch: pressing it turns bricks into coins for a short duration.
 */
public class PSwitchBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<PSwitchBlock> CODEC = simpleCodec(PSwitchBlock::new);
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    public static final int DURATION = 200;

    private static final int RADIUS_XZ = 24;
    private static final int RADIUS_Y = 12;

    /**
     * Bricks converted by each active switch, so {@link #tick} can put them back.
     * Server-side only and short-lived; entries are removed when the switch pops back up.
     */
    private static final Map<GlobalPos, List<BlockPos>> CONVERTED = new ConcurrentHashMap<>();

    public PSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PRESSED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRESSED);
    }

    /**
     * Puts the bricks back when the switch stops existing, however it went.
     *
     * <p>This used to hang off {@code playerWillDestroy}, which only covers a player breaking
     * the block by hand. An explosion, a piston, or {@code /setblock} left the switch's entry in
     * {@link #CONVERTED} with no scheduled tick to consume it, so the map leaked and the coins
     * stayed coins forever. {@code affectNeighborsAfterRemoval} is the hook vanilla's own
     * buttons and pressure plates use to undo their effect, and it runs on every removal path.
     *
     * <p>It fires only when the block *type* changes ({@code LevelChunk} gates it on
     * {@code !oldState.is(newBlock)}), so pressing and releasing the switch does not trip it.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        revertConverted(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    /** Turns this switch's coins back into bricks and forgets them. */
    private static void revertConverted(Level level, BlockPos pos) {
        List<BlockPos> converted = CONVERTED.remove(GlobalPos.of(level.dimension(), pos.immutable()));
        if (converted == null) {
            return;
        }
        for (BlockPos target : converted) {
            // Only revert blocks the player has not already consumed or replaced.
            if (level.getBlockState(target).getBlock() instanceof CoinBlock) {
                level.setBlock(target, ModBlocks.BRICK_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        activate(level, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        activate(level, pos, state);
        return InteractionResult.SUCCESS;
    }

    private void activate(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PRESSED)) {
            return;
        }
        level.setBlock(pos, state.setValue(PRESSED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.scheduleTick(pos, this, DURATION);

        // Remember exactly which bricks we converted so the effect can be undone. Without
        // this the "short duration" swap would be permanent.
        List<BlockPos> converted = BlockAreaScan.findMatching(level, pos, RADIUS_XZ, RADIUS_Y,
                target -> target.getBlock() instanceof BrickBlock);
        for (BlockPos target : converted) {
            level.setBlock(target, ModBlocks.COIN_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        if (!converted.isEmpty()) {
            CONVERTED.put(GlobalPos.of(level.dimension(), pos.immutable()), converted);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PRESSED)) {
            return;
        }
        level.setBlock(pos, state.setValue(PRESSED, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 1.0F, 0.8F);
        revertConverted(level, pos);
    }
}
