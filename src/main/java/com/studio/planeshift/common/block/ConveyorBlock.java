package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.PlaneShiftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;

/**
 * Conveyor belt: pushes any entity standing on it in the direction it faces.
 */
public class ConveyorBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);

    /**
     * How quickly an entity is eased toward the belt's drift speed, per tick.
     *
     * <p>Low enough that the belt reads as a surface rather than a shove, and that a player who
     * starts walking against it wins within a few ticks.
     */
    private static final double EASE = 0.22D;

    protected static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public ConveyorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.HorizontalDirectionalBlock> codec() {
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

    /**
     * Eases anything standing on the belt toward the belt's drift speed.
     *
     * <p>The original implementation added a fixed impulse every tick while the entity was under
     * a threshold, which is not a conveyor — it is an accelerator. It ramped anything standing on
     * it up to the threshold and held it there, so a player could not stand still on a belt at all
     * and could barely walk against one.
     *
     * <p>Easing toward a target instead gives the belt a terminal speed it cannot exceed, and
     * leaves the player's own movement free to dominate it: walking against a belt at
     * {@code conveyorSpeed} blocks per tick simply wins, which is what makes a belt an obstacle
     * to work with rather than a surface to be thrown off.
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        double speed = PlaneShiftConfig.SERVER.conveyorSpeed.get();
        if (speed <= 0.0D) {
            return;
        }

        Direction facing = state.getValue(FACING);
        Vec3 current = entity.getDeltaMovement();
        double targetX = facing.getStepX() * speed;
        double targetZ = facing.getStepZ() * speed;

        // Only the axis the belt actually runs along is touched. Nudging the other one would
        // push a 2.5D player off their rail, which the server would then have to snap back.
        double newX = facing.getStepX() != 0 ? ease(current.x, targetX) : current.x;
        double newZ = facing.getStepZ() != 0 ? ease(current.z, targetZ) : current.z;

        entity.setDeltaMovement(newX, current.y, newZ);
        entity.hurtMarked = true;
    }

    /**
     * One step of the belt's pull: move {@code current} a fraction of the way to {@code target}.
     *
     * <p>Pure and public so the belt's two defining properties can be tested without a world:
     * it converges on the target from either side, and it never overshoots it. The old
     * implementation had neither property, which is why standing on a belt was impossible.
     */
    public static double ease(double current, double target) {
        return current + (target - current) * EASE;
    }
}
