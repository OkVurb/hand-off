package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A platform you jump up <em>through</em> and then stand on.
 *
 * <p>The most conspicuous absence in the whole block set. Semisolid platforms are in essentially
 * every level of every 2D Mario game, and they are the reason those levels can stack routes on top
 * of each other: without them, any platform above you is also a ceiling, so a course can only ever
 * be a single ribbon of ground with things on it. That is exactly what PlaneShift's courses were.
 *
 * <p>The implementation is vanilla's own trick for one-way collision. {@link CollisionContext}
 * knows where the colliding entity is relative to a shape, so the block simply reports a full cube
 * to anything above it and nothing at all to anything below. No ticking, no entity tracking, no
 * state — the collision box is a function of who is asking.
 *
 * <p>Sneaking drops you through, which is the Minecraft convention for exactly this and costs
 * nothing to support: a player who has climbed onto a semisolid by mistake would otherwise have to
 * walk to the end of it.
 */
public class SemisolidBlock extends Block {

    public static final MapCodec<SemisolidBlock> CODEC = simpleCodec(SemisolidBlock::new);

    public SemisolidBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /**
     * Solid from above, empty from below.
     *
     * <p>{@code isAbove} with {@code canAscend = false} is the whole mechanic: it is true only when
     * the entity's feet are at or above the top face, so a jump arc passes straight through and a
     * descent lands on it.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            // Deliberately drop through when sneaking, the way a Minecraft player expects to get
            // off a platform they did not mean to be on.
            if (entity != null && entity.isCrouching()) {
                return Shapes.empty();
            }
        }
        return context.isAbove(Shapes.block(), pos, false) ? Shapes.block() : Shapes.empty();
    }

    /**
     * The visual and interaction shape is always the full block.
     *
     * <p>Only the <em>collision</em> shape is conditional. If this were conditional too, the
     * platform would flicker out of existence in the block outline as the player jumped past it,
     * and it would stop being selectable in creative from underneath.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return Shapes.block();
    }

    /**
     * Never occludes.
     *
     * <p>A semisolid is something you can be inside, so the renderer must not cull the faces of
     * whatever is behind it — and the player's own head is regularly in one on the way up.
     */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}
