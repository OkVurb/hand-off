package com.studio.planeshift.server;

import com.studio.planeshift.common.block.BlockAreaScan;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModFluids;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The water level is a variable, not a constant.
 *
 * <p>§4.4, and the plan calls it the strongest vindication of having built the fluid as a fluid: a
 * flooded room in the reference shows a surface line partway up it that <em>moves during the
 * level</em>. A pool at a fixed height is scenery the player swims through once; a pool that rises
 * is a clock, and everything in the room has to be read against it.
 *
 * <h2>One layer, and only on top of water that is already there</h2>
 *
 * <p>The tide adds and removes a single layer at the surface it finds, and it will not place water
 * anywhere that does not already have water directly beneath it. That rule is what keeps this out
 * of the reachability proof's way: the proof is run against the generated canvas, so anything the
 * tide does at runtime is invisible to it, and the only safe thing to do with an invisible change
 * is to make it one that cannot break a route. Water is both passable and swimmable to the solver,
 * so a column that gains a layer stays crossable; a column that loses one returns to exactly the
 * geometry that was proved.
 *
 * <p>It also means the tide cannot escape its pool. A flooded stretch in the middle of a dry course
 * rises and falls within its own banks, because the layer above dry land has no water under it.
 */
public final class TideService {

    /** How often the tide is recomputed. Two seconds: it is a tide, not a strobe. */
    private static final int INTERVAL = 40;

    /** Ticks for a full rise and fall. */
    private static final int PERIOD = 400;

    /** How far around the player the surface is looked for and moved. */
    private static final int RANGE_XZ = 20;
    private static final int RANGE_Y = 10;

    private TideService() {
    }

    /** Moves the surface, if this player is standing in a course that has one. */
    public static void tick(ServerPlayer player, CourseState state) {
        if (!state.inCourse() || player.tickCount % INTERVAL != 0) {
            return;
        }
        Level level = player.level();
        Block water = ModFluids.WATER_BLOCK.get();
        List<BlockPos> pool = BlockAreaScan.findMatching(level, player.blockPosition(),
                RANGE_XZ, RANGE_Y, found -> found.is(water));
        if (pool.isEmpty()) {
            return;
        }

        int surface = Integer.MIN_VALUE;
        for (BlockPos pos : pool) {
            surface = Math.max(surface, pos.getY());
        }

        // High half of the cycle: add a layer. Low half: take one away. The layer being added and
        // the layer being removed are the same one, so the pool oscillates between two heights
        // rather than climbing away over a long session.
        boolean rising = (level.getGameTime() % PERIOD) < PERIOD / 2;
        BlockState fill = water.defaultBlockState();
        for (BlockPos pos : pool) {
            if (pos.getY() != surface) {
                continue;
            }
            BlockPos above = pos.above();
            if (rising) {
                if (level.getBlockState(above).isAir()) {
                    level.setBlock(above, fill, Block.UPDATE_ALL);
                }
            } else if (level.getBlockState(above).is(water)
                    && level.getBlockState(above.above()).isAir()) {
                // Only the topmost layer comes off, or a deep pool would drain from the middle.
                level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
