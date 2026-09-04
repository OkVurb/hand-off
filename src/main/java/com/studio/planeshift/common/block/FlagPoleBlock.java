package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.server.CourseCompletionService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A Mario-style flag pole at the end of a course.
 *
 * <p>Walking through it triggers completion on the logical server. It has no collision so
 * the player passes straight through, like a ribbon.
 */
public class FlagPoleBlock extends Block {

    public enum Part implements StringRepresentable {
        BASE("base"),
        POLE("pole"),
        TOP("top");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final MapCodec<FlagPoleBlock> CODEC = simpleCodec(FlagPoleBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();
    private static final long COOLDOWN = 40; // 2 seconds

    /** Upper bound on the downward walk in {@link #heightBand}. The generator builds eight. */
    private static final int MAX_BAND_SCAN = 16;

    public FlagPoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.POLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean wasInside) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        long now = level.getGameTime();
        long last = LAST_TRIGGER.getOrDefault(player.getUUID(), -COOLDOWN);
        if (now - last < COOLDOWN) {
            return;
        }
        LAST_TRIGGER.put(player.getUUID(), now);

        // Pay for the height first: onComplete reads the running score to work out the end-of-course
        // bonuses and then resets, so anything added after the slide is added to a banked total.
        com.studio.planeshift.server.CourseScoringService.awardFlagpole(player, heightBand(level, pos),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        CourseCompletionService.beginSlide(player, pos);
    }

    /**
     * How far up the pole this block sits, counting from the base.
     *
     * <p>Measured by walking down rather than by reading {@link Part}, because PART only
     * distinguishes BASE, POLE and TOP — six of the eight blocks share the POLE value, and those
     * six are exactly the ones the player is choosing between. The information the payout needs is
     * the height, and the height is only recoverable from the stack.
     *
     * <p>Bounded so a malformed or player-built pole cannot walk the world downward forever.
     */
    private static int heightBand(Level level, BlockPos pos) {
        int band = 0;
        BlockPos cursor = pos.below();
        while (band < MAX_BAND_SCAN && level.getBlockState(cursor).getBlock() instanceof FlagPoleBlock) {
            band++;
            cursor = cursor.below();
        }
        return band;
    }

    public static void clear(UUID playerId) {
        LAST_TRIGGER.remove(playerId);
    }
}
