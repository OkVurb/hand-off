package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModSounds;
import com.studio.planeshift.server.CourseStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Muncher: Indestructible black spiked plant terrain hazard.
 */
public class MuncherBlock extends Block {

    public static final MapCodec<MuncherBlock> CODEC = simpleCodec(MuncherBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public MuncherBlock(Properties properties) {
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
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            CourseState cs = CourseStateAccess.get(player);
            if (cs.inCourse() && !cs.invulnerable(level.getGameTime())) {
                player.hurtServer((ServerLevel) level, level.damageSources().cactus(), 2.0F);
                level.playSound(null, pos, ModSounds.DAMAGE.get(), SoundSource.BLOCKS, 0.8F, 1.2F);
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
