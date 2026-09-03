package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * A ? block in the Mario style: hit it from below to pop a pickup.
 *
 * <p>Once used it stays empty but remains a solid platform, so courses stay readable.
 * The actual pickup is resolved by the server; the client just sees the bump and drop.
 */
public class QuestionBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<QuestionBlock> CODEC = simpleCodec(QuestionBlock::new);
    public static final BooleanProperty USED = BooleanProperty.create("used");

    public QuestionBlock(Properties properties) {
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
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || state.getValue(USED) || !(player instanceof ServerPlayer)) {
            return;
        }
        if (HitFromBelowBlock.isHeadContact(player, pos)) {
            level.setBlock(pos, state.setValue(USED, true), Block.UPDATE_ALL);
            popPickup(level, pos);
            level.playSound(null, pos, ModSounds.QUESTION_BUMP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Public hook for jumping into the block from below. */
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        attack(state, level, pos, player);
    }

    /**
     * Triggers the block from a non-player impact (e.g. a sliding Koopa shell).
     * Bypasses the {@link HitFromBelowBlock#isHeadContact} check since the impacting
     * entity is not a player standing beneath the block.
     */
    public void triggerFromImpact(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide() || state.getValue(USED)) {
            return;
        }
        level.setBlock(pos, state.setValue(USED, true), Block.UPDATE_ALL);
        popPickup(level, pos);
        level.playSound(null, pos, ModSounds.QUESTION_BUMP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    static void popPickup(Level level, BlockPos pos) {
        net.minecraft.world.item.Item pickup = switch (level.getRandom().nextInt(20)) {
            case 0, 1, 2 -> ModItems.COIN.get();
            case 3 -> ModItems.SUPER_MUSHROOM.get();
            case 4 -> ModItems.EXTRA_PIP.get();
            case 5 -> ModItems.FIRE_FLOWER.get();
            case 6 -> ModItems.ICE_FLOWER.get();
            case 7 -> ModItems.LEAF.get();
            case 8 -> ModItems.PROPELLER_MUSHROOM.get();
            case 9 -> ModItems.ACORN.get();
            case 10 -> ModItems.CLOUD_FLOWER.get();
            case 11 -> ModItems.HAMMER.get();
            case 12 -> ModItems.BOOMERANG.get();
            case 13 -> ModItems.TANOOKI.get();
            case 14 -> ModItems.MEGA_MUSHROOM.get();
            case 15 -> ModItems.MINI_MUSHROOM.get();
            case 18 -> ModItems.POISON_MUSHROOM.get();
            case 16 -> ModItems.THREE_UP.get();
            case 17 -> ModItems.FIVE_UP.get();
            case 19 -> ModItems.CAT_SUIT.get();
            default -> ModItems.STAR_POWER.get();
        };
        ItemEntity drop = new ItemEntity(level,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                new ItemStack(pickup));
        drop.setPickUpDelay(0);
        drop.setDeltaMovement(0.0D, 0.28D, 0.0D);
        level.addFreshEntity(drop);
    }
}
