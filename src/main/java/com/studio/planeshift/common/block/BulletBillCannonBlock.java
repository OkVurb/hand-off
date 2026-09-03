package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.entity.BulletBillEntity;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Bullet Bill Blaster: fires Bullet Bills horizontally along the course.
 */
public class BulletBillCannonBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<BulletBillCannonBlock> CODEC = simpleCodec(BulletBillCannonBlock::new);
    private static final int FIRE_INTERVAL = 80; // 4 seconds

    public BulletBillCannonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.WEST));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, FIRE_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 16.0D, false);
        if (nearest != null) {
            double distSq = nearest.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distSq > 4.0D) {
                shoot(level, pos, facing);
            }
        }
        level.scheduleTick(pos, this, FIRE_INTERVAL);
    }

    private void shoot(ServerLevel level, BlockPos pos, Direction facing) {
        double spawnX = pos.getX() + 0.5D + facing.getStepX() * 0.75D;
        double spawnY = pos.getY() + 0.25D;
        double spawnZ = pos.getZ() + 0.5D + facing.getStepZ() * 0.75D;

        BulletBillEntity bullet = new BulletBillEntity(ModEntities.BULLET_BILL.get(), level);
        bullet.setPos(spawnX, spawnY, spawnZ);
        float yaw = facing.toYRot();
        bullet.setYRot(yaw);
        bullet.setYHeadRot(yaw);
        level.addFreshEntity(bullet);

        level.playSound(null, pos, ModSounds.FIREBALL.get(), SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnX, spawnY + 0.25D, spawnZ, 8, 0.1D, 0.1D, 0.1D, 0.03D);
    }
}
