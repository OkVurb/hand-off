package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The climbable stalk grown by {@link SecretVineBlock}.
 *
 * <p>Separate from the sprouting block because the two do different jobs: the sprouter is a hidden
 * trigger that grows once, while this is ordinary climbable scenery that may exist in quantity.
 * Keeping them apart also means a vine can be placed by hand in a template without dragging the
 * growth state machine along with it.
 */
public class CourseVineBlock extends Block {

    public static final MapCodec<CourseVineBlock> CODEC = simpleCodec(CourseVineBlock::new);

    public CourseVineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** Climbable: this is the whole point of the block. */
    @Override
    public boolean isLadder(BlockState state, net.minecraft.world.level.LevelReader level,
                            BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        return true;
    }

    /** No collision, so the player climbs through it rather than standing on it. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.empty();
    }
}
