package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Donut Block: holds your weight for a moment, then drops out from under you.
 *
 * <p>Three phases, driven entirely by scheduled ticks so no per-tick scanning is needed:
 * <ol>
 *   <li><b>Idle</b> — solid and stable.</li>
 *   <li><b>Shaking</b> — a player has stood on it. Lasts {@link #SHAKE_TICKS}, which is the
 *       warning; the {@code shaking} state drives a distinct model so the tell is visible.</li>
 *   <li><b>Gone</b> — removed, then restored after {@link #RESPAWN_TICKS} so a course can be
 *       retried without the bridge being permanently destroyed.</li>
 * </ol>
 *
 * <p>Restoring rather than dropping a falling-block entity is deliberate: a player who misses the
 * jump respawns at a checkpoint and needs the bridge to still be there. A permanently destroyed
 * donut bridge would soft-lock the course.
 */
public class DonutBlock extends Block {

    public static final MapCodec<DonutBlock> CODEC = simpleCodec(DonutBlock::new);

    /** True once a player has stood on it and the countdown has begun. */
    public static final BooleanProperty SHAKING = BooleanProperty.create("shaking");
    /** 0 while present, 1 while the block is absent and waiting to return. */
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 1);

    /** Warning window before the block falls. Long enough to react, short enough to threaten. */
    private static final int SHAKE_TICKS = 20;
    /** How long the gap stays open before the block comes back. */
    private static final int RESPAWN_TICKS = 60;

    public DonutBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SHAKING, false).setValue(PHASE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAKING, PHASE);
    }

    /**
     * Starts the countdown when a player stands on top.
     *
     * <p>Only players trigger it. An enemy patrolling across a donut bridge collapsing it would
     * remove the player's route through no fault of their own.
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide() || !(entity instanceof Player)) {
            return;
        }
        if (state.getValue(SHAKING) || state.getValue(PHASE) != 0) {
            return;
        }
        level.setBlock(pos, state.setValue(SHAKING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, SHAKE_TICKS);
        level.playSound(null, pos, ModSounds.QUESTION_BUMP.get(), SoundSource.BLOCKS, 0.6F, 0.6F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PHASE) == 0) {
            // Shake finished: open the gap and schedule the return.
            level.setBlock(pos, state.setValue(SHAKING, false).setValue(PHASE, 1), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, RESPAWN_TICKS);
            level.playSound(null, pos, ModSounds.BRICK_BREAK.get(), SoundSource.BLOCKS, 0.7F, 1.3F);
            return;
        }
        // Gap expired: restore, so a retried course still has its bridge.
        level.setBlock(pos, state.setValue(SHAKING, false).setValue(PHASE, 0), Block.UPDATE_ALL);
    }

    /**
     * Whether the block is currently passable. Phase 1 keeps the block in the world so its
     * scheduled tick survives, but renders and collides as nothing.
     */
    public static boolean isOpen(BlockState state) {
        return state.hasProperty(PHASE) && state.getValue(PHASE) == 1;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(
            BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return isOpen(state)
                ? net.minecraft.world.phys.shapes.Shapes.empty()
                : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(
            BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return isOpen(state)
                ? net.minecraft.world.phys.shapes.Shapes.empty()
                : super.getShape(state, level, pos, context);
    }
}
