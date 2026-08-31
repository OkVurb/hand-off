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
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Coin ring block: hit from below to release a ring of coins.
 */
public class CoinRingBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<CoinRingBlock> CODEC = simpleCodec(CoinRingBlock::new);
    public static final BooleanProperty USED = BooleanProperty.create("used");

    public CoinRingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(USED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(USED);
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer) || state.getValue(USED)) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0D) * Math.PI * 2.0D;
            double dx = Math.cos(angle) * 0.4D;
            double dz = Math.sin(angle) * 0.4D;
            ItemEntity drop = new ItemEntity(level,
                    pos.getX() + 0.5D + dx, pos.getY() + 1.1D, pos.getZ() + 0.5D + dz,
                    new ItemStack(ModItems.COIN.get()));
            drop.setPickUpDelay(0);
            drop.setDeltaMovement(dx * 0.2D, 0.28D, dz * 0.2D);
            level.addFreshEntity(drop);
        }
        level.playSound(null, pos, ModSounds.COIN_PICKUP.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
        level.setBlock(pos, state.setValue(USED, true), Block.UPDATE_ALL);
    }
}
