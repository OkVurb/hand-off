package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.course.ToadHouseGifts;
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
 * A prize box in a Toad House. Open one and the rest close.
 *
 * <p>A Toad House is not three power-ups, it is a <em>choice</em> of one, and that difference is
 * the entire mechanic. Three question blocks in a room would have been three rewards for walking
 * into a room, which is worth less than the single unconditional grant it replaced — more generous
 * and less interesting at the same time.
 *
 * <p>Not a reskinned {@link QuestionBlock}, deliberately. A question block rolls on a table that
 * includes coins and joke items, and it exists to be hit in passing during a course. This is the
 * thing the player travelled here for, so it always pays a real Form, and it is drawn to read as
 * furniture belonging to the room rather than as level scenery.
 */
public class ToadBoxBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<ToadBoxBlock> CODEC = simpleCodec(ToadBoxBlock::new);

    /** Set on every box in the room once any one of them is opened. */
    public static final BooleanProperty USED = BooleanProperty.create("used");

    /**
     * How far the closing sweep reaches.
     *
     * <p>Comfortably larger than the room the generator builds, and small enough that two Toad
     * Houses could never see each other. Measured in blocks on each axis from the box that was
     * opened.
     */
    private static final int CLOSE_RADIUS = 24;

    public ToadBoxBlock(Properties properties) {
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
        if (level.isClientSide() || state.getValue(USED)) {
            return;
        }
        if (!HitFromBelowBlock.isHeadContact(player, pos)) {
            return;
        }
        open(level, pos);
    }

    /** Public hook for jumping into the block from below, matching QuestionBlock. */
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        attack(state, level, pos, player);
    }

    /**
     * Pays out, then closes every other box in the room.
     *
     * <p>The sweep runs before the payout is popped so the player cannot open a second box in the
     * same tick that the first one is still resolving.
     */
    private void open(Level level, BlockPos pos) {
        closeAll(level, pos);

        ItemEntity drop = new ItemEntity(level,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                new ItemStack(ToadHouseGifts.roll(level.getRandom())));
        drop.setPickUpDelay(0);
        drop.setDeltaMovement(0.0D, 0.28D, 0.0D);
        level.addFreshEntity(drop);

        level.playSound(null, pos, ModSounds.POWER_UP.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    /** Marks every box within reach as used, including the one that was hit. */
    private void closeAll(Level level, BlockPos origin) {
        BlockPos min = origin.offset(-CLOSE_RADIUS, -CLOSE_RADIUS, -CLOSE_RADIUS);
        BlockPos max = origin.offset(CLOSE_RADIUS, CLOSE_RADIUS, CLOSE_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ToadBoxBlock && !state.getValue(USED)) {
                // immutable(): betweenClosed hands out a shared mutable cursor, and setBlock keeps
                // the reference. Without this every box would be recorded at the last position the
                // loop visited.
                level.setBlock(pos.immutable(), state.setValue(USED, true), Block.UPDATE_ALL);
            }
        }
    }
}
