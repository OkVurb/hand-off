package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Reward family: prize cache (Design Bible, "Blocks, objects, hazards, and portals").
 *
 * <p>Contract: "Server grants once per ledger rule; clear pickup feedback." Deterministic
 * payout — no random drops behind progression. The cache visibly empties (OPENED) and
 * never refills on its own, matching the readable-interactable pattern kept from the
 * prototype audit.
 */
public class PrizeCacheBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<PrizeCacheBlock> CODEC = simpleCodec(PrizeCacheBlock::new);
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    /** Deterministic payout: five Coins, always. */
    public static final int COIN_PAYOUT = 5;

    public PrizeCacheBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPENED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPENED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (state.getValue(OPENED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(OPENED, true), Block.UPDATE_ALL);
            popCoins(level, pos);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Also opens when struck from below — the classic stomp-from-underneath read. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(OPENED) && !level.isClientSide()
                && HitFromBelowBlock.isHeadContact(player, pos)) {
            level.setBlock(pos, state.setValue(OPENED, true), Block.UPDATE_ALL);
            popCoins(level, pos);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.3F);
        }
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        attack(state, level, pos, player);
    }

    private static void popCoins(Level level, BlockPos pos) {
        for (int i = 0; i < COIN_PAYOUT; i++) {
            ItemEntity coin = new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                    new ItemStack(ModItems.COIN.get()));
            coin.setDeltaMovement(
                    (level.random.nextDouble() - 0.5D) * 0.25D,
                    0.28D + level.random.nextDouble() * 0.12D,
                    (level.random.nextDouble() - 0.5D) * 0.25D);
            level.addFreshEntity(coin);
        }
    }
}
