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

    private static final java.util.Map<java.util.UUID, Long> COOLDOWNS = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && player.isCrouching()) {
            long lastWarp = COOLDOWNS.getOrDefault(player.getUUID(), 0L);
            if (level.getGameTime() - lastWarp > 40) { // 2 second cooldown
                boolean isReturnPipe = level.getBlockState(pos.above(50)).getBlock() == this;
                BlockPos targetPos = isReturnPipe ? pos.above(50) : pos.below(50);
    
                if (!isReturnPipe && level.getBlockState(targetPos).isAir()) {
                    // Generate an underground sub-room (9x6x9)
                    for (int x = -4; x <= 4; x++) {
                        for (int y = -1; y <= 4; y++) {
                            for (int z = -4; z <= 4; z++) {
                                BlockPos p = targetPos.offset(x, y, z);
                                if (x == -4 || x == 4 || y == -1 || y == 4 || z == -4 || z == 4) {
                                    level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
                                } else {
                                    level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                                }
                            }
                        }
                    }
                    // Place the return pipe
                    level.setBlockAndUpdate(targetPos, this.defaultBlockState());
                    // Coins in the room.
                    //
                    // Item entities, not blocks. There is no coin *block* — ModBlocks.COIN does
                    // not exist, which is what stopped this file compiling. A coin in PlaneShift
                    // is ModItems.COIN, picked up by walking into it, and the pickup path is what
                    // credits the player's coin count. Spawned the same way CourseWriter spawns
                    // the generator's own coins, including the generated tag, so the course
                    // cleanup pass removes these along with everything else it built.
                    for (int x = -2; x <= 2; x += 2) {
                        for (int z = -2; z <= 2; z += 2) {
                            if (x == 0 && z == 0) {
                                continue; // leave the return pipe clear
                            }
                            BlockPos coinPos = targetPos.offset(x, 1, z);
                            net.minecraft.world.entity.item.ItemEntity coin =
                                    new net.minecraft.world.entity.item.ItemEntity(level,
                                            coinPos.getX() + 0.5D, coinPos.getY() + 0.25D,
                                            coinPos.getZ() + 0.5D,
                                            new net.minecraft.world.item.ItemStack(
                                                    com.studio.planeshift.common.registry.ModItems.COIN.get()));
                            coin.setPickUpDelay(0);
                            coin.setDeltaMovement(0.0D, 0.0D, 0.0D);
                            coin.addTag(com.studio.planeshift.server.gen.SegmentLibrary.GENERATED_TAG);
                            level.addFreshEntity(coin);
                        }
                    }
                }
    
                COOLDOWNS.put(player.getUUID(), level.getGameTime());
                player.teleportTo(targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5);
                level.playSound(null, targetPos, net.minecraft.sounds.SoundEvents.UI_TOAST_IN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(5) == 0) {
            boolean hasLink = level.getBlockState(pos.above(50)).getBlock() == this || level.getBlockState(pos.below(50)).getBlock() == this;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.1;
            double z = pos.getZ() + 0.5;
            if (hasLink) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y, z, 0, 0.05, 0);
            } else {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 0, 0.05, 0);
            }
        }
    }
}
