package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The course's own lava.
 *
 * <p>Replaces {@code Blocks.LAVA}, which was the last vanilla block the generator placed. Two
 * reasons, and the second is the one that matters.
 *
 * <p><b>It looked like a different game.</b> Every other material in a course is drawn by
 * {@code BlockTextureGen} in one flat-shaded style and now carries this mod's conventions --
 * the lighting ramp, connected edges, aerial perspective. Vanilla lava carries none of them.
 *
 * <p><b>Vanilla lava is a fluid, and a fluid moves.</b> A generated course places lava in a pit and
 * expects it to stay in that pit. Real lava spreads: it flows over the lip, runs along the floor
 * the composer carefully proved traversable, and turns a hazard the player can see into one that
 * arrives. Worse, it does that <em>after</em> {@code CourseReachability} has signed off on the
 * layout, so the proof that a course is completable stops describing the course the player is
 * standing in. A platformer's lava is a surface, not a liquid.
 *
 * <p>So this is a solid block that never moves, with no collision, that hurts anything inside it.
 * It reads as lava and behaves like a hazard tile, which is what the genre has always actually
 * meant by lava.
 */
public class CourseLavaBlock extends Block {

    public static final MapCodec<CourseLavaBlock> CODEC = simpleCodec(CourseLavaBlock::new);

    /**
     * Damage per tick of contact.
     *
     * <p>High, and deliberately not instant death. The pip damage model means a player with a Form
     * loses it and gets a moment to jump out, which is the difference between a hazard and a
     * trapdoor -- but stay in and the second tick finishes the job.
     */
    public static final float DAMAGE = 6.0F;

    public CourseLavaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /**
     * Nothing stands on lava.
     *
     * <p>No collision at all, so the player falls in rather than landing on a surface that then
     * hurts them. A hazard you can stand on top of is a platform with a bad temper.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.empty();
    }

    /** Still a full cube to look at, so the pit reads as filled rather than as a hole. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean wasInside) {
        if (level instanceof ServerLevel server) {
            entity.hurtServer(server, level.damageSources().hotFloor(), DAMAGE);
        }
    }

    /**
     * Whether a position holds lava of any kind.
     *
     * <p>Exists because {@code BowserEntity} killed itself with {@code isInLava()} -- a vanilla
     * fluid check that this block, being solid, does not answer. The castle's whole payoff is the
     * axe dropping the bridge and the boss falling into the pit, and that mechanic would have
     * broken silently the moment the pit stopped being a fluid: no error, no failing test, just a
     * boss that lands in the lava and stands there.
     */
    public static boolean isLava(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof CourseLavaBlock
                || state.is(net.minecraft.world.level.block.Blocks.LAVA);
    }
}
