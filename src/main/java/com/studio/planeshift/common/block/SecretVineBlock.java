package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hidden block that sprouts a climbable vine when hit from below, revealing a route upward.
 *
 * <p>The vine grows one segment per scheduled tick rather than appearing whole, so the player
 * watches the secret open and understands they are meant to follow it. The growth is what turns a
 * hidden block into an invitation.
 *
 * <p>Growth is capped at {@link #MAX_HEIGHT} and stops early at any obstruction, so a vine placed
 * under a low ceiling simply grows short instead of overwriting whatever is above it.
 */
public class SecretVineBlock extends Block implements HitFromBelowBlock {

    public static final MapCodec<SecretVineBlock> CODEC = simpleCodec(SecretVineBlock::new);

    /** Whether the vine has already sprouted; a spent block is inert. */
    public static final BooleanProperty SPROUTED = BooleanProperty.create("sprouted");
    /** Segments grown so far, so growth survives a save. */
    public static final IntegerProperty GROWN = IntegerProperty.create("grown", 0, 15);

    /** Tallest a vine will grow. Enough to reach a coin heaven above the course ceiling. */
    private static final int MAX_HEIGHT = 15;
    /** Ticks between segments. */
    private static final int GROW_INTERVAL = 2;

    public SecretVineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SPROUTED, false).setValue(GROWN, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPROUTED, GROWN);
    }

    @Override
    public void attemptHitFromBelow(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || state.getValue(SPROUTED)) {
            return;
        }
        if (!HitFromBelowBlock.isHeadContact(player, pos)) {
            return;
        }
        level.setBlock(pos, state.setValue(SPROUTED, true).setValue(GROWN, 0), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, GROW_INTERVAL);
        level.playSound(null, pos, ModSounds.POWER_UP.get(), SoundSource.BLOCKS, 0.8F, 1.4F);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        attemptHitFromBelow(state, level, pos, player);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SPROUTED)) {
            return;
        }
        int grown = state.getValue(GROWN);
        if (grown >= MAX_HEIGHT) {
            return;
        }

        BlockPos next = pos.above(grown + 1);
        if (!level.getBlockState(next).isAir()) {
            // Something is in the way; stop cleanly rather than carving through it.
            return;
        }

        level.setBlock(next, ModBlocks.COURSE_VINE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, state.setValue(GROWN, grown + 1), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, GROW_INTERVAL);
        level.playSound(null, next, ModSounds.QUESTION_BUMP.get(), SoundSource.BLOCKS, 0.3F, 1.9F);
    }

    /** Invisible and non-solid until hit, exactly like a hidden question block. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(SPROUTED) ? super.getCollisionShape(state, level, pos, context)
                : Shapes.empty();
    }
}
