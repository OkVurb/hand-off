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

/**
 * A Mario-style flag pole at the end of a course.
 *
 * <p>Walking through it triggers completion on the logical server. It has no collision so
 * the player passes straight through, like a ribbon.
 */
public class FlagPoleBlock extends Block {

    public static final MapCodec<FlagPoleBlock> CODEC = simpleCodec(FlagPoleBlock::new);
    private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();
    private static final long COOLDOWN = 40; // 2 seconds

    public FlagPoleBlock(Properties properties) {
        super(properties);
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
        CourseCompletionService.onComplete(player);
    }

    public static void clear(UUID playerId) {
        LAST_TRIGGER.remove(playerId);
    }
}
