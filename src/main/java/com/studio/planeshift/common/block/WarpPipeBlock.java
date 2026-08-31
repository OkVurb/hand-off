package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.server.CourseService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Warp Pipe — "Mario-style transport. Right-click to dive into course_1."
 *
 * <p>Vertical-slice stand-in for pipe-linked sub-areas. Course authors can place
 * these to hide bonus rooms or connect zones.
 */
public class WarpPipeBlock extends Block {

    public static final MapCodec<WarpPipeBlock> CODEC = simpleCodec(WarpPipeBlock::new);

    public WarpPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CourseService.loadCourse(serverPlayer, "course_1");
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}
