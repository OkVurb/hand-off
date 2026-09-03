package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
 * A breakable brick in the Mario style: hit it from below and it shatters — usually.
 *
 * <p>Some bricks are coin bricks. Those do not break: they pop a coin, darken, and stay solid.
 * Which bricks those are is decided by position rather than by a die roll, so a course plays the
 * same way every attempt. A brick that broke on one run and paid a coin on the next would make
 * every route unlearnable, and route memory is most of what a platformer rewards.
 *
 * <p>No block drops by design — the fun is clearing the path, not grinding resources.
 */
public class BrickBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<BrickBlock> CODEC = simpleCodec(BrickBlock::new);

    /** A coin brick that has already paid out. Solid, darker, and now inert. */
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");

    /** Roughly one brick in this many is a coin brick. */
    private static final int COIN_BRICK_ONE_IN = 4;

    public BrickBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SPENT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPENT);
    }

    /**
     * Whether the brick at {@code pos} is a coin brick.
     *
     * <p>A hash of the coordinates, so it is fixed for a given position and needs nothing stored
     * in the block state or the level. The multipliers are odd and coprime so neighbouring bricks
     * in a row do not all land on the same answer — a wall of eight bricks should contain a
     * couple of coin bricks scattered through it, not eight of one kind.
     */
    public static boolean isCoinBrick(BlockPos pos) {
        int hash = pos.getX() * 73_856_093 ^ pos.getY() * 19_349_663 ^ pos.getZ() * 83_492_791;
        return Math.floorMod(hash, COIN_BRICK_ONE_IN) == 0;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return;
        }
        // Bricks react to any head contact, regardless of player state. Deliberately not gated on
        // a "Super" size: a block that sometimes ignores a correct hit reads as broken rather than
        // as a rule, and course routing depends on bricks always being clearable.
        if (!HitFromBelowBlock.isHeadContact(player, pos)) {
            return;
        }

        if (state.getValue(SPENT)) {
            // Already paid out. Still solid, so it stays part of the route; the bump is the only
            // feedback, which is how the player learns this one is finished.
            level.playSound(null, pos, ModSounds.QUESTION_BUMP.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            return;
        }

        if (isCoinBrick(pos)) {
            payCoin(level, pos);
            level.setBlock(pos, state.setValue(SPENT, true), Block.UPDATE_ALL);
            return;
        }

        level.destroyBlock(pos, false);
        level.playSound(null, pos, ModSounds.BRICK_BREAK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** Pops a coin out of the top of the brick, the way a question block does. */
    public static void payCoin(Level level, BlockPos pos) {
        ItemEntity coin = new ItemEntity(level,
                pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                new ItemStack(ModItems.COIN.get()));
        coin.setDeltaMovement(0.0D, 0.28D, 0.0D);
        coin.setPickUpDelay(6);
        level.addFreshEntity(coin);
        level.playSound(null, pos, ModSounds.COIN_PICKUP.get(), SoundSource.BLOCKS, 0.9F, 1.4F);
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        attack(state, level, pos, player);
    }
}
