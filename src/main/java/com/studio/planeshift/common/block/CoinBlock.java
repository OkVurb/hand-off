package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Coin block: hit from below to release coins. Holds up to 5 coins before emptying.
 */
public class CoinBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<CoinBlock> CODEC = simpleCodec(CoinBlock::new);
    public static final IntegerProperty COINS = IntegerProperty.create("coins", 0, 5);

    public CoinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COINS, 5));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COINS);
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        payOne(state, level, pos);
    }

    /**
     * Pays out one coin, if any are left.
     *
     * <p>Split out from {@link #attemptHitFromBelow} so an impact that is not a head bump — a
     * ground pound landing on the block, a Koopa shell running into it — can trigger the same
     * behaviour. Those callers cannot go through {@code attemptHitFromBelow} because it requires
     * a {@link Player} and, for most of these blocks, a head-contact test the impacting thing
     * fails by definition.
     */
    public static void payOne(BlockState state, Level level, BlockPos pos) {
        int remaining = state.getValue(COINS);
        if (remaining > 0) {
            ItemEntity drop = new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                    new ItemStack(ModItems.COIN.get()));
            drop.setPickUpDelay(0);
            drop.setDeltaMovement(0.0D, 0.28D, 0.0D);
            level.addFreshEntity(drop);
            level.playSound(null, pos, ModSounds.COIN_PICKUP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            // Task 55: Coin particles flying upward
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // The mod registers its own COIN_SPARKLE for exactly this. Reaching for a vanilla
                // particle when a purpose-built one exists is how everything ends up looking the same.
                serverLevel.sendParticles(com.studio.planeshift.common.registry.ModParticles.COIN_SPARKLE.get(),
                        pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                        8, 0.2D, 0.3D, 0.2D, 0.05D);
            }
            level.setBlock(pos, state.setValue(COINS, remaining - 1), Block.UPDATE_ALL);
        }
    }
}
