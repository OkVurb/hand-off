package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

public class TrampolineBlock extends Block {

    public static final MapCodec<TrampolineBlock> CODEC = simpleCodec(TrampolineBlock::new);
    public static final double LAUNCH_VELOCITY = 1.35D;
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.6D, 1.0D);

    public TrampolineBlock(Properties properties) {
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
        if (entity instanceof LivingEntity living) {
            living.resetFallDistance();
        }
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        Vec3 velocity = entity.getDeltaMovement();
        entity.setDeltaMovement(velocity.x * 1.15D, LAUNCH_VELOCITY, velocity.z * 1.15D);
        entity.hurtMarked = true;
        if (entity.level() instanceof ServerLevel sl) {
            sl.playSound(null, entity.blockPosition(), ModSounds.SPRING.get(), SoundSource.BLOCKS, 1.2F, 1.4F);
            sl.sendParticles(ModParticles.HIT_BURST.get(), entity.getX(), entity.getY() + 0.5D, entity.getZ(), 8, 0.2D, 0.1D, 0.2D, 0.05D);
        }
    }
}
