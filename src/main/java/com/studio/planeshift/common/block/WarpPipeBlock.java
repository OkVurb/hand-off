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

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && player.isCrouching()) {
            // Determine if we are going down or up
            // Using a simple logic: if we are high up, go down. If low, go up.
            // But an easier way is to check blockstate or just use a fixed offset.
            // Let's use an offset of 50. If there's a WarpPipeBlock 50 blocks above, we go up. Otherwise go down.
            boolean isReturnPipe = level.getBlockState(pos.above(50)).getBlock() == this;
            BlockPos targetPos = isReturnPipe ? pos.above(50) : pos.below(50);

            if (!isReturnPipe && level.getBlockState(targetPos).isAir()) {
                // Generate a simple platform and return pipe
                level.setBlockAndUpdate(targetPos, this.defaultBlockState());
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        level.setBlockAndUpdate(targetPos.below().offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    }
                }
                // clear some space above
                for (int y = 1; y <= 3; y++) {
                    level.setBlockAndUpdate(targetPos.above(y), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }

            player.teleportTo(targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5);
        }
        super.stepOn(level, pos, state, entity);
    }
}
