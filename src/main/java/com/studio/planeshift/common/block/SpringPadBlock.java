package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Movement family: spring pad (Design Bible, "Blocks, objects, hazards, and portals").
 *
 * <p>Contract: "Predictable cycle, pause behavior, camera-safe path." The launch
 * strength is constant — landing on a pad always produces the same arc, so courses can
 * be authored around it. Fall damage through the pad is always cancelled.
 */
public class SpringPadBlock extends Block {

    public static final MapCodec<SpringPadBlock> CODEC = simpleCodec(SpringPadBlock::new);

    /** Fixed launch impulse: roughly a 5-block apex at 20 TPS, before jump-assist. */
    public static final double LAUNCH_VELOCITY = 1.05D;

    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

    public SpringPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        // Absorb the landing entirely; the pad is a safe surface by contract.
        if (entity instanceof LivingEntity living) {
            living.resetFallDistance();
        }
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        // Match vanilla slime-block semantics: sneaking suppresses the bounce, anything
        // else always gets the same fixed arc. Gating on downward speed instead would
        // silently swallow the launch when simply walking onto the pad, because a
        // grounded entity's per-tick fall speed never reaches the threshold.
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        Vec3 velocity = entity.getDeltaMovement();
        entity.setDeltaMovement(velocity.x, LAUNCH_VELOCITY, velocity.z);
        entity.hurtMarked = true;
        if (entity.level() instanceof Level lvl && !lvl.isClientSide()) {
            lvl.playSound(null, entity.blockPosition(), SoundEvents.SLIME_BLOCK_FALL,
                    SoundSource.BLOCKS, 1.0F, 1.4F);
        }
    }
}
